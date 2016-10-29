<?php

$fp = fopen("data.txt", 'w');
fwrite($fp, "Last request: " . date("Y-m-d h:i:sa"));

if (isset($_REQUEST['From']) && isset($_REQUEST['Body'])) {
    header("content-type: text/xml");
    $sender = $_REQUEST['From'];
    $message = $_REQUEST['Body'];
    $response = "SENDER=" . $sender . "|MESSAGE=" . $message;
} elseif(isset($_REQUEST['From'])) {
    header("content-type: text/xml");
    $response = "Please play our game - Brainy Bird!";
?>

<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Message><?php echo $response; ?></Message>
</Response>

<?php } else { ?>

<html>
<head>
    <title>Brainy Bird</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
        body {
            text-align: center;
            font-family: sans-serif;
        }
        
        h1 {
            position: relative;
            top: 40%;
        }
    </style>
</head>
<body>
    <h1>Please play our game - Brainy Bird!</h1>
</body>
</html>

<?php } ?>


