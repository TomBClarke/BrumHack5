<?php 
    header("content-type: text/xml"); 
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"; 
    $response = "";

    $sender = $_REQUEST['From'];
    $message = $_REQUEST['Body'];
    
    if ($sender == "" || $message == "") {
        $response = "Please play our game - Brainy Bird";
    } else {
        $response = "SENDER=" . $sender . "|";
        if (strpos($message, '<Score>') !== false) {
            try {
                $scoreXml = new SimpleXMLElement($message);
                $response = $response . "Score:" . $scoreXml->Score;
            } catch (Exception $e) {
                $response = $response . "Score:error";
            }
        } else {
            $response = $response . "Query:Should check for '" . $message . "' for their score.";
        }
    }
?>
<Response>
    <Message>
        <?php echo $response; ?>
    </Message>
</Response>