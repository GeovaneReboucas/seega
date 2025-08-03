# Sistema de Chat Baseado em Localização

Este projeto implementa um sistema de chat baseado em localização em Java, utilizando Sockets para comunicação síncrona e ActiveMQ para comunicação assíncrona.

## Funcionalidades

- Comunicação síncrona entre usuários online e dentro de um raio de distância.
- Comunicação assíncrona para usuários offline ou fora do raio de comunicação.
- Lista de contatos atualizada dinamicamente com base na proximidade.
- Atualização de localização, status (online/offline) e raio de comunicação.
- Interface gráfica de usuário (GUI) com componentes nativos do Java (Swing).

## Pré-requisitos

- **Java Development Kit (JDK) 11 ou superior:** Necessário para compilar e executar o projeto.
- **Apache ActiveMQ Classic:** Utilizado como o Message Oriented Middleware (MOM).

## Estrutura do Projeto

```
.
├── bin/                      # Diretório para os arquivos .class compilados
├── lib/
│   └── activemq-all-5.18.3.jar # Biblioteca do ActiveMQ
├── scripts/
│   └── ├── compile.bat                # Script para compilar o projeto
│       ├── run-client.bat             # Script para executar o cliente
│       ├── run-server.bat             # Script para executar o servidor
│       ├── start-activemq.bat         # Script para iniciar o ActiveMQ
├── src/
│   └── ├── client/       # Código do cliente (GUI)
│       ├── model/        # Classes de modelo (User, Location, Message)
│       ├── mq/           # Classes para interação com ActiveMQ
│       ├── server/       # Código do servidor
│       └── util/         # Classes utilitárias (DistanceCalculator)
├── apache-activemq-5.19.0/   # Instalação do ActiveMQ
└── README.md                 # Este arquivo
```

## Como Executar

Siga os passos abaixo para configurar e executar a aplicação.

### 1. Instale o Java

Certifique-se de que o JDK 11 ou superior está instalado e configurado no seu sistema.

### 2. Baixe e Configure o ActiveMQ

O ActiveMQ já está incluído no diretório `apache-activemq-5.19.0/`. Não é necessário baixar novamente.

### 3. Compile o Projeto

Abra um terminal na raiz do projeto e execute o script de compilação:

```bash
chmod +x compile.sh
./compile.sh
```

Este comando irá compilar todos os arquivos `.java` e colocar os arquivos `.class` no diretório `bin/`.

### 4. Inicie o ActiveMQ

Em um novo terminal, inicie o broker do ActiveMQ:

```bash
chmod +x start_activemq.sh
./start_activemq.sh
```

O ActiveMQ estará rodando em background. Você pode acessar a console de administração em `http://localhost:8161/` (usuário: `admin`, senha: `admin`).

### 5. Inicie o Servidor do Chat

Em outro terminal, inicie o servidor do chat:

```bash
chmod +x run_server.sh
./run_server.sh
```

O servidor começará a escutar por conexões de clientes na porta `12345`.

### 6. Inicie o Cliente do Chat

Para cada usuário, abra um novo terminal e execute o cliente:

```bash
chmod +x run_client.sh
./run_client.sh
```


