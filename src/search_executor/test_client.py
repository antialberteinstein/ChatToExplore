import socket
import sys


def main():
    HOST = "localhost"
    PORT = 8887

    query = "Andy Robertson"

    try:
        with socket.create_connection((HOST, PORT), timeout=10) as s:
            # Send query terminated by newline
            s.sendall((query + "\n").encode("utf-8"))

            # Read response
            resp = b""
            while True:
                chunk = s.recv(4096)
                if not chunk:
                    break
                resp += chunk

        # Print decoded response (keep original newlines)
        print(resp.decode("utf-8"), end="")

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
