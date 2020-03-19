docker stop hermes-envoy
docker rm hermes-envoy
docker build -t hermes-envoy -f ./Dockerfile .
docker run -d -p 9091:9091 --env ENVOY_PORT=9091 --env HERMES_HOST=host.docker.internal --env HERMES_PORT=6565 --name hermes-envoy hermes-envoy