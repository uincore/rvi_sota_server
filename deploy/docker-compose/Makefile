down:
	docker-compose -f docker-compose.yml -f core-rvi.yml -f client-rvi.yml -f sota-client.yml down
server-up:
	docker-compose -f docker-compose.yml -f core-rvi.yml up -d
client-up:
	docker-compose -f docker-compose.yml -f core-rvi.yml -f client-rvi.yml up -d
	sleep 20
	docker run --net=host -d --name=sota-client  -p 8888:8888  -p 9080:9080  -e RUST_LOG="debug"  -e DEVICE_UUID="$(DEVICE_UUID)" -v $(CURDIR)/toml:/tmp advancedtelematic/sota-client
rvi-client-logs:
	docker logs dockercompose_rvi_device_1
sota-client-logs:
	docker logs dockercompose_sota-client_1
