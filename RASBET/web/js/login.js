


document.getElementById('form_login').onsubmit = function (event){
    let username = document.getElementById('username').value;
    let password = document.getElementById('password').value;

    var formObject =  {"login": [username,password]};
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
        if(obj.validate){
          
          localStorage.setItem("username",username);

          if(obj.mode == 0)
            window.location = 'page_apostador.html';
          else if (obj.mode == 1)
            window.location = 'page_especialista.html';
          else if (obj.mode == 2)
            window.location = 'page_admin.html';
        }
        else{
          alert("Username ou password Inválida");
        }
    });


    event.preventDefault();

}
