<?php
	try {
	// Connexion
	$bdd = new PDO("mysql:host=172.17.0.2;dbname=FindThisFace", "root", "admin");
	} catch (PDOException $erreur) { // Gestion des erreurs
	echo "<p>Erreur : " . $erreur->getMessage() . "</p>\n";
}

function GetName($bdd, $id)
{
    $wiki = "Aucune information";
    $req=$bdd->prepare("SELECT *
                        FROM ID
                        WHERE IDPhoto = :id
                        LIMIT 1");
    $req->execute(array(
        'id'=>$id
    ));

	foreach($req as $local)
		$wiki = $local['LienWiki'];
	
    $req->closeCursor();

    return $wiki;
}

//echo GetName($bdd, $id);
//echo "<p>".GetName($bdd, $id)."</p>";

//$personne = GetName($bdd, $id)->fetchAll();
//echo "resultat est : ".$personne;


?>
