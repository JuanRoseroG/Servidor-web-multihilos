import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class Main {

    public void init() throws IOException{
        ServerSocket server = new ServerSocket(8050);
        var isAlive = true;
        while (isAlive) {
            System.out.println("Esperando cliente...");
            var socket = server.accept();
            System.out.println("¡Cliente conectado!");
            dispatchWorker(socket);
        }


    }

    public void dispatchWorker(Socket socket) throws IOException{

        new Thread(
                ()->{
                    try {
                        handleRequest(socket);
                    } catch (IOException e) {
                        throw  new RuntimeException(e);
                    }

                }
        ).start();

    }

    public void handleRequest(Socket  socket) throws IOException{
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("GET")){
                var resource = line.split(" ")[1].replace("/","");
                System.out.println("El cliente anda pidiendo: "+resource);
                sendResponse(socket, resource);


            }

        }
    }

    private String contentType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }



    public void sendResponse(Socket socket, String resource) throws IOException {
        File file = new File("resources/" + resource);

        if (!file.exists()) {
            File errorFile = new File("resources/404.html");
            String errorHtml = "<html><body><h1>404 Not Found</h1></body></html>";

            if (errorFile.exists()) {
                errorHtml = new String(Files.readAllBytes(errorFile.toPath()));
            }

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("HTTP/1.1 404 Not Found\r\n");
            writer.write("Content-Type: text/html\r\n");
            writer.write("Content-Length: " + errorHtml.length() + "\r\n");
            writer.write("Connection: close\r\n");
            writer.write("\r\n");
            writer.write(errorHtml);
            writer.flush();
            writer.close();
            socket.close();
            return;
        }

        String mime = contentType(resource);
        long fileLength = file.length();

        OutputStream out = new BufferedOutputStream(socket.getOutputStream());

        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mime + "\r\n" +
                "Content-Length: " + fileLength + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(header.getBytes());

        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytes;
            while ((bytes = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }
            out.flush();
        }

        out.close();
        socket.close();
    }


    public static void main(String[] args) throws IOException {

        Main main = new Main();
        main.init();


        //Response
//
//
//        server.close();
    }
}
