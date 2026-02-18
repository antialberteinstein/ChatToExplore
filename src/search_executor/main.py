import dotenv
import logging
import socket

dotenv.load_dotenv()

from gemini_search import search, NoInternetError, NoSearchResultsError



def handle_client(conn, addr):
	try:
		logging.info("Connected by %s", addr)
		f = conn.makefile('rwb')
		# Read first line (command or simple query)
		first = f.readline()
		if not first:
			return
		first_line = first.decode('utf-8', errors='ignore').strip()

		# Treat the first line as the query (simple newline-terminated query protocol)
		query = first_line
		logging.info("Received query: %s", query)

		try:
			answer = search(query)

			if answer is None:
				out = "Xin lỗi, tôi không tìm thấy thông tin phù hợp.\n"
			else:
				single_line = " ".join(answer.splitlines()).strip()
				out = single_line + "\n"
				logging.info("Search answer: %s", single_line)

		except NoInternetError:
			out = "Không có thông tin.\n"
		except NoSearchResultsError:
			out = "Không có thông tin.\n"
		except Exception:
			logging.exception("Unexpected error while searching")
			out = "Không có thông tin.\n"

		conn.sendall(out.encode('utf-8'))

	except Exception:
		logging.exception("Handler error")
	finally:
		try:
			conn.close()
		except Exception:
			pass


def run_server(host: str, port: int):
	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
	sock.bind((host, port))
	sock.listen(5)
	logging.info("Search TCP server listening on %s:%d", host, port)

	try:
		while True:
			conn, addr = sock.accept()
			# Simple sequential handling (keep it minimal as requested)
			handle_client(conn, addr)

	except KeyboardInterrupt:
		logging.info("Shutting down server")
	finally:
		sock.close()


def main():
	PORT = 8887
	HOST = "localhost"

	logging.basicConfig(level=logging.INFO, format="[%(asctime)s] %(levelname)s: %(message)s")
	run_server(HOST, PORT)

if __name__ == "__main__":
	main()