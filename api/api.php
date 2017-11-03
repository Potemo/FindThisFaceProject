<?php 
//if(isset($_FILES['file'])) { 
        $max_size = 2097152; // cela vaut à 2 MB
        // Les extensions permises 
        $extensions = array("jpeg", "jpg", "png"); 
        // Le chemin où l'on va uploader notre fichier
        $path_dir = "/var/www/html/images/"; 
        $file_name = basename($_FILES['file']['name']); 
        $path_dir = $path_dir . $file_name;

        $file_size = $_FILES['file']['size']; 
        $file_tmp = $_FILES['file']['tmp_name']; 
        $file_ext=strtolower(end(explode('.',$_FILES['file']['name']))); 
        $error = false; // On vérifie si l'extension du fichier upload est permise

        if(in_array($file_ext,$extensions) === false) { 
                $error = true; $response['message'] = "L'extension du fichier n'est pas permise"; 
        } // On vérifie si notre fichier ne dépasse pas la limite autoris�

//      if($file_size > $max_size) {
//              $error = true;
//              $response['message'] = "Votre fichier ne doit pas dépasser 2 MB";
//      }
    $test = 99;

    if (!$error && move_uploaded_file($file_tmp, $path_dir)) {
        $response['success'] = 1;
        $response['message'] = 'Upload reussi';
	$i = 1;
	do {
    	$codePython = "/root/openface/demos/compare.py /var/www/html/images/photo.jpg /root/openface/images/examples/".$i.".jpg >> /var/log/openface.log";
//        exec($codePython,$output,$result);
	exec($codePython,$output,$result);
	$resultData = json_decode($result, true);
	$idPersonne = 0;
	// error_log("result = " + $result);
	if ($result < $test) {
	//	echo "resultData est inférieurr a test" >> /var/log/api.log;
	//	echo "test = ". $test >> /var/log/api.log;
	//	echo "resultData = ". $resultData >> /var/log/api.log;
	//	echo " ------------------------------------------------- " >> /var/log/api.log;
//		error_log("result = " + $resultData);
//		error_log("test = " + $test);
		$test = $result;
		$idPersonne = $i;
		}
	$i++;
	} while ($i <= 2);
	include "connectDB.php";
	$name = GetName($bdd, $idPersonne);
	$response['message'] = $name;
//	$codePython = "/root/openface/demos/compare.py /var/www/html/images/photo.jpg /root/openface/images/examples/* >> /var/log/openface.log";
//      exec($codePython);  
    }
    else {
        $response['success'] = 0;
        $reponse['message'] = 'Upload echoue';
    }
 
    echo json_encode($response);
 
//} else {

//      echo ("ERREUR - Pas d'image");

//}

?>
