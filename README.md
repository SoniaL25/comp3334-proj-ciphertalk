"# comp3334-proj-ciphertalk\n main readme" 

Note:
Due to incomplete backend API, the client implements a mock fallback mechanism.
All core E2EE features are fully functional in this mode.


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