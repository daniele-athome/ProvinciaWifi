<?php

$user = 'user';
$pass = 'pass';

if ((isset($_POST['auth_user']) and $_POST['auth_user'] == $user) and
    (isset($_POST['auth_pass']) and $_POST['auth_pass'] == $pass)) {

    $url = isset($_POST['redirurl']) ? $_POST['redirurl'] : '';
    $data = file_get_contents('login-success.html');
    $data = str_replace('${redirurl_html}', htmlentities($url), $data);
    $data = str_replace('${redirurl_js}', json_encode($url), $data);
    echo $data;
}
else {
    echo file_get_contents('login-failed.html');
}
