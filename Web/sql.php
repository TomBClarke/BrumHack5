<?php

function connect() {
	$db = mysqli_connect("leaderboard.cn5ic4glplxe.us-west-2.rds.amazonaws.com", "test", "testing123", "Leaderboard");
    return $db;
}

function close ($db) {
    mysqli_close($db);
}

function submitScore($number, $name, $score) {
    $alreadyInDB = false;
    $betterScore = false;
    $msgString = "";
    $db = connect();
    
    $sql = sprintf("SELECT player_result FROM Results WHERE player_phone = '%s';",
		mysqli_real_escape_string($db, $number)
	);
	$result = mysqli_query($db, $sql);
    
    if (mysqli_num_rows($result) > 0) {
        $alreadyInDB = true;
        $row = mysqli_fetch_array($result);
        $betterScore = intval($score) > intval($row['player_result']);
    }
    
    if (!$alreadyInDB) {
        // Insert new
        $sql = sprintf("INSERT INTO Results VALUES (default, '%s', '%s', %s);",
            mysqli_real_escape_string($db, $name),
            mysqli_real_escape_string($db, $number),
            mysqli_real_escape_string($db, $score)
        );
        $msgString = "Inserted phone number, name and score.";
    } elseif ($betterScore) {
        // Update
        $sql = sprintf("UPDATE Results SET player_name = '%s', player_result = %s WHERE player_phone = '%s';",
            mysqli_real_escape_string($db, $name),
            mysqli_real_escape_string($db, $score),
            mysqli_real_escape_string($db, $number)
        );
        $msgString = "Updated name/score.";
    }
    
	$result = mysqli_query($db, $sql);
    close($db);
    
    // Probably done/fine...
    return "Successfully submitted score. " . $msgString;
}

function getScore($senderNumber, $queryNumber) {
    $db = connect();
    $sql = sprintf("SELECT * FROM Results WHERE player_phone = '%s' or player_phone = '%s';",
		mysqli_real_escape_string($db, $senderNumber),
		mysqli_real_escape_string($db, $queryNumber)
	);
	$result = mysqli_query($db, $sql);
    close($db);
    
    if (!$result or mysqli_num_rows($result) == 0 or mysqli_num_rows($result) == 1) {
        return "User does not exsist.";
    }
    
    $senderString = " Your score is ";
    $queryString = "'s score is ";
    while($row = mysqli_fetch_array($result)) {
    	if ($row['player_phone'] == $senderNumber) {
            $senderString = $senderString . $row['player_result'] . ".";
        }
    	if ($row['player_phone'] == $queryNumber) {
            $queryString = $row['player_name'] . $queryString . $row['player_result'] . ".";
        }
    		
    }
    
    return $queryString . $senderString;
}

?>