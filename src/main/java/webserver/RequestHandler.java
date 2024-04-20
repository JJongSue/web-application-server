package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final String rootPath = "webapp";

    private Socket connection;

    private DataBase dataBase;

    public RequestHandler(Socket connection, DataBase dataBase) {
        this.connection = connection;
        this.dataBase = dataBase;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String request = bufferedReader.readLine();
            log.debug("request", request);

            String[] requests = request.split(" ");
            byte[] body = "Hello World".getBytes();
            DataOutputStream dos = new DataOutputStream(out);

            if(requests.length >= 2) {
                if(!requests[1].equals("/")) {
                    String[] urlParts = requests[1].split("\\?");

                    String filePath = rootPath + urlParts[0];

                    Map<String, String> params = new HashMap();
                    if(urlParts.length >= 2) {
                        params = HttpRequestUtils.parseQueryString(urlParts[1]);
                    }

                    if(urlParts[0].equals("/user/create")) {
                        String userId = params.get("userId");
                        String password = params.get("password");
                        String name = params.get("name");
                        String email = params.get("email");
                        User user = new User(userId, password, name, email);

                        dataBase.addUser(user);
                        body = user.toString().getBytes();
                    } else if(urlParts[0].equals("/user/findAll")) {
                        body = dataBase.findAll().toString().getBytes();
                    }

                    else {
                        File file = new File(filePath);

                        if (!file.exists()) {
                            log.error("File not found: " + filePath);
                            String errorMessage = "File not found";
                            byte[] errorBody = errorMessage.getBytes();
                            dos = new DataOutputStream(out);
                            response404Header(dos, errorBody.length);
                            responseBody(dos, errorBody);
                            return;
                        }
                        body = Files.readAllBytes(new File(filePath).toPath());
                    }
                }
            }

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response404Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
            dos.writeBytes("Content-Type: text/plain\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
