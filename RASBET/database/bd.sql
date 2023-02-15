DROP DATABASE IF EXISTS rasbet;
CREATE DATABASE rasbet;

USE rasbet;

CREATE USER IF NOT EXISTS 'rasbet' IDENTIFIED BY 'rasbet';
GRANT ALL ON rasbet.* TO 'rasbet';

CREATE TABLE IF NOT EXISTS Utilizador (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    pass VARCHAR(50) NOT NULL,
    modo INT NOT NULL DEFAULT '0',  # 0 - apostador ; 1 - especialista ; 2 - administrador
		PRIMARY KEY (id),
        UNIQUE KEY (username)
);

CREATE TABLE IF NOT EXISTS Follow (
    id INT NOT NULL AUTO_INCREMENT,
    idUtilizador INT NOT NULL,
    idSeguidor INT NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idUtilizador)
			REFERENCES Utilizador(id),
		FOREIGN KEY (idSeguidor)
			REFERENCES Utilizador(id)
);

CREATE TABLE IF NOT EXISTS Info (
    id INT NOT NULL AUTO_INCREMENT,
    idUtilizador INT NOT NULL,
    balance DOUBLE NOT NULL DEFAULT '0.0',
    ganho DOUBLE NOT NULL DEFAULT '0.0',
    nome VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    morada VARCHAR(50) NOT NULL,
    n_tlm VARCHAR(50) NOT NULL,
    nif VARCHAR(50) NOT NULL,
    cc VARCHAR(50) NOT NULL,
    dataNascimento DATE NOT NULL,
		PRIMARY KEY (id),
        UNIQUE KEY (email),
        UNIQUE KEY (nif),
        UNIQUE KEY (cc),
        FOREIGN KEY (idUtilizador)
			REFERENCES Utilizador(id)
);

CREATE TABLE IF NOT EXISTS Evento (
    id INT NOT NULL AUTO_INCREMENT,
    desporto VARCHAR(50) NOT NULL, # example: "Futebol","Basket",....
    ocasiao VARCHAR(50) NOT NULL, # example: "Champions league",....
    tipo VARCHAR(5) NOT NULL, # 1x2 , 1 2 , 1 
		PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS Partida (
    id VARCHAR(40) NOT NULL,
    idEvento INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    estado VARCHAR(10) NOT NULL DEFAULT 'aberta',
    commenceTime DATETIME NOT NULL,
    scores VARCHAR(10) NOT NULL DEFAULT '',
    promocao DOUBLE NOT NULL DEFAULT '1.0',
		PRIMARY KEY (id),
        FOREIGN KEY (idEvento)
			REFERENCES Evento(id)
);

CREATE TABLE IF NOT EXISTS GameFollow (
    id INT NOT NULL AUTO_INCREMENT,
    idPartida VARCHAR(40) NOT NULL,
    idSeguidor INT NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idPartida)
			REFERENCES Partida(id),
		FOREIGN KEY (idSeguidor)
			REFERENCES Utilizador(id)
);

CREATE TABLE IF NOT EXISTS Equipa (
    id INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
		PRIMARY KEY (id),
        UNIQUE KEY (nome)
);

CREATE TABLE IF NOT EXISTS Odd (
    id INT NOT NULL AUTO_INCREMENT,
    idPartida VARCHAR(40) NOT NULL,
    idEquipa INT NOT NULL,
    odd DOUBLE NOT NULL DEFAULT '1.0',
		PRIMARY KEY (id),
        FOREIGN KEY (idPartida)
			REFERENCES Partida(id),
		FOREIGN KEY (idEquipa)
			REFERENCES Equipa(id)
);

CREATE TABLE IF NOT EXISTS Result (
    id INT NOT NULL AUTO_INCREMENT,
    idPartida VARCHAR(40) NOT NULL,
    idEquipa INT, # null para draw ????
		PRIMARY KEY (id),
        FOREIGN KEY (idPartida)
			REFERENCES Partida(id),
		FOREIGN KEY (idEquipa)
			REFERENCES Equipa(id)
);

CREATE TABLE IF NOT EXISTS Bookmaker (
	id INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(30) NOT NULL,
    idPartida VARCHAR(40) NOT NULL,
    lastUpdate DATETIME NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idPartida)
			REFERENCES Partida(id)
);

CREATE TABLE IF NOT EXISTS Market (
	id INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(30) NOT NULL,
    idBookmaker INT NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idBookmaker)
			REFERENCES Bookmaker(id)
);

CREATE TABLE IF NOT EXISTS Outcome (
    id INT NOT NULL AUTO_INCREMENT,
    idMarket INT NOT NULL,
    idEquipa INT NOT NULL,
    outcome DOUBLE NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idMarket)
			REFERENCES Market(id),
		FOREIGN KEY (idEquipa)
			REFERENCES Equipa(id)
);

CREATE TABLE IF NOT EXISTS Aposta (
    id INT NOT NULL AUTO_INCREMENT,
    idUtilizador INT NOT NULL,
    tipo INT NOT NULL,
    montante DOUBLE NOT NULL,
    odd DOUBLE NOT NULL DEFAULT '1.0',
    dataAposta DATETIME NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    won BOOLEAN NOT NULL DEFAULT false,
		PRIMARY KEY (id),
        FOREIGN KEY (idUtilizador)
			REFERENCES Utilizador(id)
);

CREATE TABLE IF NOT EXISTS Previsao (
    id INT NOT NULL AUTO_INCREMENT,
    idAposta INT NOT NULL,
    idPartida VARCHAR(40) NOT NULL,
    idPrevisao INT NOT NULL,
		PRIMARY KEY (id),
        FOREIGN KEY (idAposta)
			REFERENCES Aposta(id),
		FOREIGN KEY (idPartida)
			REFERENCES Partida(id),
		FOREIGN KEY (idPrevisao)
			REFERENCES Equipa(id)
);

CREATE TABLE IF NOT EXISTS Notificacao (
    id INT NOT NULL AUTO_INCREMENT,
    idUtilizador INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    dataHora DATETIME NOT NULL,
    viewed BOOLEAN NOT NULL DEFAULT false,
		PRIMARY KEY (id),
        FOREIGN KEY (idUtilizador)
			REFERENCES Utilizador(id)
);

/*

SELECT B.idPartida,B.nome AS bookmaker,B.lastUpdate,M.nome AS market,O1.idEquipa AS equipaA,O1.outcome AS outcomeA,O2.idEquipa AS equipaB,O2.outcome AS outcomeB,O3.idEquipa AS equipaC,O3.outcome AS outcomeC FROM Bookmaker B INNER JOIN Market M ON B.id = M.idBookmaker INNER JOIN Outcome AS O1 INNER JOIN Outcome AS O2 ON O1.id != O2.id AND O1.idMarket = O2.idMarket INNER JOIN Outcome O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idMarket = O3.idMarket ON M.id = O1.idMarket GROUP BY O1.idMarket,O2.idMarket,O3.idMarket

SELECT * FROM Aposta A INNER JOIN Previsao Pr ON A.id = Pr.idAposta INNER JOIN Partida Pa ON Pr.idPartida=Pa.id INNER JOIN (Odd AS O1 INNER JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida AND O1.idPartida != '1' AND O2.idPartida != '1') ON Pa.id = O1.idPartida GROUP BY O1.idPartida,O2.idPartida;
SELECT A.id,A.tipo,A.montante,A.odd,A.dataAposta FROM Aposta A INNER JOIN Utilizador U WHERE username = 'a';

SELECT Pr.idPartida,E.nome AS previsao,Eq1.nome AS 'HomeTeam',Eq2.nome AS 'AwayTeam', Pa.scores 
	FROM Previsao Pr INNER JOIN Partida Pa ON Pr.idPartida=Pa.id INNER JOIN 
		Equipa AS E ON Pr.idPrevisao = E.id INNER JOIN ((Partida P INNER JOIN
			(Odd AS O1 INNER JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida AND O1.idEquipa != '1' AND O2.idEquipa != '1') ON P.id = O1.idPartida) INNER JOIN
					Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id)
						WHERE Pr.idPartida = O1.idPartida AND Pr.idAposta = '1' GROUP BY O1.idPartida,O2.idPartida ORDER BY Pr.id ASC;

SELECT P.id,completed,estado,commenceTime,scores,O1.idEquipa AS 'idHomeTeam',Eq1.nome AS 'homeTeam',O1.odd AS 'homeOdd',O2.idEquipa AS 'idAwayTeam',Eq2.nome AS 'awayTeam',O2.odd AS 'awayOdd',O3.idEquipa AS 'idDrawTeam',Eq3.nome AS 'drawTeam',O3.odd AS 'drawOdd' FROM (Partida P INNER JOIN ((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida) LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida AND O3.idEquipa = '1') ON P.id = O1.idPartida) INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id INNER JOIN Equipa Eq3 ON O3.idEquipa = Eq3.id WHERE P.idEvento = '1' AND P.completed =true AND P.estado = 'fechada'
                                                 GROUP BY O1.idPartida,O2.idPartida,O3.idPartida;

SELECT * FROM Aposta A INNER JOIN Previsao Pr ON A.id = Pr.idAposta INNER JOIN Partida Pa ON Pr.idPartida=Pa.id INNER JOIN (Odd AS O1 INNER JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida AND O1.idPartida != '1' AND O2.idPartida != '1') ON Pa.id = O1.idPartida GROUP BY O1.idPartida,O2.idPartida;
SELECT tipo,montante,odd,dataAposta FROM Aposta;
SELECT nome FROM Previsao Pr INNER JOIN Partida Pa ON Pr.idPartida=Pa.id INNER JOIN Equipa AS E ON Pr.idPrevisao = E.id;

SELECT id FROM Aposta WHERE idUtilizador = '1' AND dataAposta = '2022-11-15 16:42:07';

SELECT P.id,completed,estado,commenceTime,scores,O1.idEquipa AS 'idHomeTeam',Eq1.nome AS 'homeTeam',O1.odd AS 'homeOdd',O2.idEquipa AS 'idAwayTeam',Eq2.nome AS 'awayTeam',O2.odd AS 'awayOdd',O3.idEquipa AS 'idDrawTeam',Eq3.nome AS 'drawTeam',O3.odd AS 'drawOdd' FROM (Partida P INNER JOIN ((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida) LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida AND O3.idEquipa = '1') ON P.id = O1.idPartida) INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id INNER JOIN Equipa Eq3 ON O3.idEquipa = Eq3.id WHERE P.idEvento = '1' AND P.estado='fechada' GROUP BY O1.idPartida,O2.idPartida,O3.idPartida;

SELECT U.id,I.balance FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE username='a';


SELECT P.id,completed,estado,commenceTime,scores,O1.idEquipa AS 'idHomeTeam',Eq1.nome AS 'homeTeam',O1.odd AS 'homeOdd',O2.idEquipa AS 'idAwayTeam',Eq2.nome AS 'awayTeam',O2.odd AS 'awayOdd',O3.idEquipa AS 'idDrawTeam',Eq3.nome AS 'drawTeam',O3.odd AS 'drawOdd' FROM (Partida P INNER JOIN ((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida) LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida AND O3.idEquipa = '1') ON P.id = O1.idPartida) INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id INNER JOIN Equipa Eq3 ON O3.idEquipa = Eq3.id WHERE P.idEvento = '1' AND P.completed =false AND P.estado = 'aberta'
                                                 GROUP BY O1.idPartida,O2.idPartida,O3.idPartida;

SELECT * FROM Previsao;
SELECT * FROM Aposta;
SELECT * FROM Utilizador;
SELECT * FROM Info;
SELECT * FROM Partida;
SELECT * FROM Evento;
SELECT * FROM Bookmaker;
SELECT * FROM Market;
SELECT * FROM Outcome;
SELECT * FROM Odd;
SELECT * FROM Equipa;
SELECT * FROM Notificacao;
SELECT * FROM Follow;
SELECT * FROM GameFollow;

SELECT balance FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE username='a';

SELECT odd FROM Odd U WHERE idPartida='3ec6ffeee46fb782d327e8b42463b00c' AND idEquipa ='2';

SELECT U.username,U.pass,I.balance,I.nome,I.email,I.morada,I.n_tlm,I.nif,I.cc,I.dataNascimento FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE U.id = '1';

SELECT B.nome AS bookmaker,B.lastUpdate,M.nome AS market,O.idEquipa,O.outcome FROM Bookmaker B LEFT JOIN Market M ON B.id = M.idBookmaker 
	LEFT JOIN Outcome O ON M.id = O.idMarket 
		WHERE idPartida = '0dff082f36f877c198e4c1890a26ffbe';
        
SELECT B.nome AS bookmaker,B.lastUpdate,M.nome AS market,O1.idEquipa AS equipaA,O1.outcome AS outcomeA,O2.idEquipa AS equipaB,O2.outcome AS outcomeB,O3.idEquipa AS equipaC,O3.outcome AS outcomeC FROM Bookmaker B LEFT JOIN Market M ON B.id = M.idBookmaker INNER JOIN Outcome AS O1 INNER JOIN Outcome AS O2 ON O1.id != O2.id AND O1.idMarket = O2.idMarket INNER JOIN Outcome O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idMarket = O3.idMarket ON M.id = O1.idMarket WHERE idPartida = 'b317a08504832adc4166d53126acc53e' GROUP BY O1.idMarket,O2.idMarket,O3.idMarket;
        
SELECT * FROM Market WHERE idBookmaker = '15';

SELECT O1.idEquipa,O1.odd,O2.idEquipa,O2.odd,O3.idEquipa,O3.odd FROM 
	(Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id != O2.id AND O1.idPartida = O2.idPartida) 
		LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida GROUP BY O1.idPartida,O2.idPartida,O3.idPartida;

SELECT Eq.nome,Eq2.nome FROM 
	((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id != O2.id AND O1.idPartida = O2.idPartida)
		INNER JOIN Equipa Eq ON O1.idEquipa = Eq.id)
			INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id;
    
SELECT P.id,completed,estado,commenceTime,desporto,ocasiao,nome,odd FROM 
	((Partida P INNER JOIN Evento E ON P.idEvento = E.id) INNER JOIN Odd O ON P.id = O.idPartida) INNER JOIN Equipa Eq ON O.idEquipa = Eq.id;

SELECT P.id,completed,estado,commenceTime,scores,O1.idEquipa AS 'idHomeTeam',Eq1.nome AS 'homeTeam',O1.odd AS 'homeOdd',
	O3.idEquipa AS 'idDrawTeam',Eq3.nome AS 'drawTeam',O3.odd AS 'drawOdd',O2.idEquipa AS 'idAwayTeam',Eq2.nome AS 'awayTeam',O2.odd AS 'awayOdd' 
		FROM (Partida P INNER JOIN ((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida) 
			LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida AND O3.idEquipa = '1') ON P.id = O1.idPartida) 
				INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id INNER JOIN Equipa Eq3 ON O3.idEquipa = Eq3.id
					WHERE P.idEvento = '1' AND P.completed = false GROUP BY O1.idPartida,O2.idPartida,O3.idPartida;
    
SELECT * FROM Utilizador;
SELECT * FROM Info;

SELECT modo from Utilizador WHERE username = 'c' AND pass = 'a';

SELECT EXISTS(SELECT * from Utilizador WHERE username = 'b' OR pass = 'b') AS result;
INSERT INTO Utilizador (username,pass,balance,ganho,nome,email,morada,n_tlm,nif,cc,dataNascimento) SELECT * FROM 
	(SELECT 'f' AS col1,'f' AS col2,'0.0' AS col3,'0.0' AS col4,'f' AS col5,'f' AS col6,'f' AS col7,'1' AS col8,'1' AS col9,'1' AS col10,
		'1111-11-11' AS col11) AS tmp WHERE NOT EXISTS (SELECT username FROM Apostador WHERE username = 'f') LIMIT 1;

UPDATE Partida SET completed = true,estado = 'fechada',scores='3x1' WHERE id ='282fed58a147325e5b0afffcaa1a0e87';
UPDATE Partida SET completed = true,estado = 'fechada',scores='1x1' WHERE id ='923f451c2663e29d50cdb0027ab20ea7';
UPDATE Partida SET completed = true,estado = 'fechada',scores='1x3' WHERE id ='0c33b89a2f86957ebbe2584ff87754b2';

SELECT * FROM Partida WHERE id = '282fed58a147325e5b0afffcaa1a0e87' OR id = '923f451c2663e29d50cdb0027ab20ea7' OR id = '0c33b89a2f86957ebbe2584ff87754b2';

INSERT INTO Follow (idUtilizador,idSeguidor) SELECT * FROM 
	(SELECT '1' AS col1,'4' AS col2) AS tmp WHERE NOT EXISTS 
		(SELECT idUtilizador FROM Follow WHERE idUtilizador = '1' AND idSeguidor = '4') LIMIT 1;
INSERT INTO Follow (idUtilizador,idSeguidor) SELECT * FROM 
	(SELECT '1' AS col1,'5' AS col2) AS tmp WHERE NOT EXISTS 
		(SELECT idUtilizador FROM Follow WHERE idUtilizador = '1' AND idSeguidor = '5') LIMIT 1;
        
SELECT U1.username AS seguidor,U2.username AS seguido FROM Follow F INNER JOIN Utilizador U1 ON F.idSeguidor = U1.id INNER JOIN Utilizador U2 ON F.idUtilizador = U2.id 
					WHERE U2.username = 'a';
                    
SELECT U.username AS seguidor,P.id AS seguido FROM GameFollow G INNER JOIN Utilizador U ON G.idSeguidor = U.id INNER JOIN Partida P ON G.idPartida = P.id WHERE P.id = '0c33b89a2f86957ebbe2584ff87754b2';

INSERT INTO GameFollow (idPartida,idSeguidor) SELECT * FROM 
	(SELECT '0c33b89a2f86957ebbe2584ff87754b2' AS col1,'1' AS col2) AS tmp WHERE NOT EXISTS 
		(SELECT idPartida FROM GameFollow WHERE idPartida = '0c33b89a2f86957ebbe2584ff87754b2' AND idSeguidor = '1') LIMIT 1;
INSERT INTO GameFollow (idPartida,idSeguidor) SELECT * FROM 
	(SELECT '1105aef5df26f5c23bb64482498e461c' AS col1,'1' AS col2) AS tmp WHERE NOT EXISTS 
		(SELECT idPartida FROM GameFollow WHERE idPartida = '1105aef5df26f5c23bb64482498e461c' AND idSeguidor = '1') LIMIT 1;
*/
