import socket
import json


def handle_client(client_socket):
	# Receive data from Java client
	data = client_socket.recv(1024).decode('utf-8')
	print(f"Received from Java: {data}")

	# Parse the JSON data
	json_data = json.loads(data)

	# Modify the JSON data or create a new response
	response_data = {
		"status": "success",
		"received_action": json_data.get("action"),
		"response_message": "Action processed"
	}

	# Send the response back to the Java client
	client_socket.send(json.dumps(response_data).encode('utf-8'))

	# Close the connection
	client_socket.close()


def main():
	server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	server.bind(('localhost', 9999))  # Bind to localhost and port 9999
	server.listen(5)  # Listen for incoming connections

	print("Python server is listening on port 9999...")

	while True:
		# Accept a connection from the Java client
		client_socket, addr = server.accept()
		print(f"Connection established with {addr}")

		# Handle the client in a separate function
		handle_client(client_socket)


if __name__ == "__main__":
	main()
