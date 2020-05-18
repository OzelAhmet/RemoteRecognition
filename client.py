import socket
import cv2

img = cv2.imread("image.jpg")

# encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
result, enc_img = cv2.imencode('.jpg', img)
length = str(len(enc_img.tobytes()))
print("len:", length)

# HOST = 'localhost'
HOST = '192.168.1.37'  # The server's hostname or IP address
PORT = 8080  # The port used by the server


for i in range(1):
	with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
		s.connect((HOST, PORT))

		s.send((length + " ").encode())
		s.sendall(enc_img.tobytes())
		data = s.recv(1024)
		print("id:", str(i), "res:", data)
