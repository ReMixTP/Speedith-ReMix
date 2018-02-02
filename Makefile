IP=$(shell ifconfig en0 | grep inet | awk '$$1=="inet" {print $$2}')

docker-image:
	docker build -t remix/speedith .

run:
	xhost + $(IP)
	docker run --rm -it -p 5002:8080 remix/speedith  # The Plug-in command
#	docker run --rm -it -e DISPLAY=$(IP):0 -v /tmp/.X11-unix:/tmp/.X11-unix remix/speedith  # The GUI command
