# comp3334-proj-ciphertalk


## Run Application

### Without TLS

```terminal
run-server-ui.bat
```

### With TLS

1. Run the following codes
```terminal
cd server
mvn spring-boot:run
ngrok http 8080
```

2. Copy the url from output and paste into client/api.py BASE_URL

3. Run client in a separate terminal
```terminal
python -m client.client
```