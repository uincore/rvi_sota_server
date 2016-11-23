down:
	docker-compose -f docker-compose.yml -f core-rvi.yml -f client-rvi.yml -f sota-client.yml down
server-up:
	docker-compose -f docker-compose.yml -f core-rvi.yml up -d
client-up:
	docker-compose -f docker-compose.yml -f core-rvi.yml -f client-rvi.yml -f sota-client.yml up -d
rvi-client-logs:
	docker logs dockercompose_rvi_device_1
sota-client-logs:
	docker logs dockercompose_sota-client_1
