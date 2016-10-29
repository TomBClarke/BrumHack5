<?php

header("content-type: text/xml");
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

$sender = $_REQUEST['From'];
$message = $_REQUEST['Body'];

$response = "SENDER=" . $sender . "|MESSAGE=" . $message;

?>

<Response>
    <Message><?php echo $response; ?></Message>
</Response>
