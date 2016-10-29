<?php 
    header("content-type: text/xml"); 
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    $response = "";

    $sender = $_REQUEST['From'];
    $message = $_REQUEST['Body'];
    
    if ($sender == "" || $message == "") {
        $response = "Please play our game - Brainy Bird";
    } else {
        $response = "Number:" . $sender . "|";
        if (strpos($message, '<ScoreSubmit>') !== false) {
            try {
                $xml = new SimpleXMLElement($message);
                $name = $xml->Name; // If it is a different name to whta is already in the database, update it to this? Does this need to be unique? If so, could reply as to whether it was successfully updated.
                $score = (int)$xml->Score;
                
                $response = $response . "Name:" . $name . "|";
                $response = $response . "Score:" . $score;
                
                // Could maybe attach info about if it is a new high score (from DB update).
            } catch (Exception $e) {
                $response = $response . "Name:error|Score:error";
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