document.getElementById('form_register').onsubmit = function (event){
    let username = document.getElementById('username').value;
    let password = document.getElementById('password').value;
    let email = document.getElementById('email').value;
    let morada = document.getElementById('morada').value;
    let phone = document.getElementById('phone').value;
    let dt = document.getElementById('dt').value;
    let cc = document.getElementById('cc').value;
    let nome = document.getElementById('nome').value;
    let nif = document.getElementById('nif').value;







    var formObject =  {"register": [username,password,nome,email,morada,phone,nif,cc,dt]};
    console.log(JSON.stringify(formObject));

    
   
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
          window.location = 'index.html';
        }
        else{
          alert("Username já existente");
        }
    });


    event.preventDefault();

}