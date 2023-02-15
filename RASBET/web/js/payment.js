function paymc(){
    document.getElementById("mc").hidden = false;
    document.getElementById("mb").hidden = true;
    document.getElementById("pp").hidden = true;
    localStorage.setItem("metodo_pagamento",3);
}
function paymb(){
    document.getElementById("mc").hidden = true;
    document.getElementById("mb").hidden = false;
    document.getElementById("pp").hidden = true;
    localStorage.setItem("metodo_pagamento",1);
}

function paypp(){
    document.getElementById("mc").hidden = true;
    document.getElementById("mb").hidden = true;
    document.getElementById("pp").hidden = false;
    localStorage.setItem("metodo_pagamento",2);
}


document.getElementById('form_payment').onsubmit = function (event){
    document.getElementById('form_payment').submit()

    let quantidade = document.getElementById('quantidade').value;
    let movimento = localStorage.getItem("movimentoDinheiro");
    let username = localStorage.getItem("username");
    let metodo = localStorage.getItem("metodo_pagamento");

    var formObject;

    if (movimento == "depositarDinheiro")
        formObject =  {"depositarDinheiro": [username,quantidade,metodo]};
    else 
        formObject =  {"levantarDinheiro": [username,quantidade,metodo]};


    const socket = new WebSocket("ws://localhost:33333/");


    alert("Inicio de Operação");
    socket.addEventListener('open', (event) => {
        socket.send(JSON.stringify(formObject));
    });

    socket.addEventListener('message', (event) => {
        
        let obje = JSON.parse(event.data);

        if(obje.validate){
          alert("Operação realizada com sucesso")
          window.location = 'perfil.html';
        }
        else{
          alert("Operação Inválida");
        }
    });
    event.preventDefault();
    console.log("Ola");
}

