<?php 
    include("sql.php");
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
                $name = $xml->Name;
                $score = (int)$xml->Score;
                
                $dbResult = submitScore($sender, $name, $score);
                
                $response = $response . "Name:" . $name . "|";
                $response = $response . "Score:" . $score;
                $response = $response . "|" . $dbResult;
                
                // Could maybe attach info about if it is a new high score (from DB update).
            } catch (Exception $e) {
                $response = $response . "Name:error|Score:error";
            }
        } else {
            $response = getScore($sender, $message);
        }
    }
?>

<Response>
    <Message>
        <?php echo $response; ?>
    </Message>
</Response>