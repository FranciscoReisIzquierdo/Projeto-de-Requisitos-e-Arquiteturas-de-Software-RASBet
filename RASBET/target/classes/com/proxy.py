#!/usr/bin/env python
import asyncio
import websockets
import jpysocket
import socket

async def echo(websocket):
    HOST = "127.0.0.1"  # The server's hostname or IP address
    PORT = 55555 
    s : socket.socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    async for message in websocket:
        s.connect((HOST, PORT))
        print(message)
        msgsend=jpysocket.jpyencode(message) #Encript The Msg
        s.send(msgsend) 
        msgrecv=s.recv(999999) #Recieve msg
        msgrecv=msgrecv[2:].decode('UTF-8')
        print(msgrecv)
        await websocket.send(msgrecv)
        s.close()

async def main():
    print("Starting proxy!")
    async with websockets.serve(echo, "localhost", 33333):
        await asyncio.Future()  # run forever

asyncio.run(main())
