


document.getElementById('form_notificacao').onsubmit = function (event){
    let username = localStorage.getItem("username")
    //alert("Ola")
    let titulo = document.getElementById('tituloNotificacao').value;

    let texto = document.getElementById('textoNotificacao').value;

    var formObject =  {"criarNotificacao": [username,titulo,texto]};
    console.log(JSON.stringify(formObject));
    //document.write(JSON.stringify(formObject));
    //location.href='index.html';
    
   
    const socket = new WebSocket("ws://localhost:33333/");
    


    // Connection opened
    socket.addEventListener('open', (event) => {
        socket.send(JSON.stringify(formObject));
    });
    
    // Listen for messages
    socket.addEventListener('message', (event) => {
        console.log('Message from server ', event.data);
        let obj = JSON.parse(event.data);
        console.log('Message from server ', obj.validate);
        alert("Notificação gerada com sucesso!");
        document.getElementById('tituloNotificacao').value = "";
        document.getElementById('textoNotificacao').value = "";
    });

    event.preventDefault();

}