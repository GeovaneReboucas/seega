## Tarefas do Projeto

### Fase 1: Análise e planejamento da arquitetura do sistema
- [x] Definir a estrutura de classes para usuários, mensagens e localização.
- [x] Esboçar a arquitetura de comunicação síncrona (sockets).
- [x] Esboçar a arquitetura de comunicação assíncrona (ActiveMQ).
- [x] Planejar a estrutura da interface gráfica.

### Fase 2: Implementação das classes de modelo e utilitários
- [x] Implementar a classe `User` (nome, localização, status, raio).
- [x] Implementar a classe `Location` (latitude, longitude).
- [x] Implementar a classe `Message` (conteúdo, remetente, destinatário, tipo).
- [x] Implementar utilitários para cálculo de distância.

### Fase 3: Implementação do servidor de comunicação com sockets
- [x] Criar o servidor de sockets para gerenciar conexões de usuários online.
- [x] Implementar a lógica para comunicação síncrona entre usuários online.
- [x] Gerenciar a lista de usuários online e seus status.

### Fase 4: Implementação do sistema de mensagens assíncronas com ActiveMQ
- [x] Configurar a conexão com o ActiveMQ.
- [x] Implementar o envio de mensagens para a fila do ActiveMQ.
- [x] Implementar o consumo de mensagens da fila quando o usuário estiver online.

### Fase 5: Implementação da interface gráfica do usuário
- [x] Criar a janela principal da aplicação.
- [x] Adicionar componentes para exibir lista de contatos.
- [x] Adicionar componentes para enviar e receber mensagens.
- [x] Adicionar campos para atualização de localização, status e raio.

### Fase 6: Implementação da aplicação cliente principal
- [x] Conectar o cliente ao servidor de sockets.
- [x] Integrar a lógica de comunicação síncrona e assíncrona.
- [x] Atualizar a UI com base nas informações do servidor e mensagens.

### Fase 7: Criação de scripts de compilação e execução
- [x] Criar script para compilar o projeto Java (javac).
- [x] Criar script para executar o servidor.
- [x] Criar script para executar o cliente.

### Fase 8: Testes e documentação do sistema
- [x] Realizar testes de comunicação síncrona.
- [x] Realizar testes de comunicação assíncrona.
- [x] Testar atualização de localização, status e raio.
- [x] Documentar o código e as instruções de uso.

### Fase 9: Entrega dos arquivos e instruções ao usuário
- [ ] Empacotar todos os arquivos necessários.
- [ ] Fornecer instruções claras para compilação e execução.
- [ ] Entregar o projeto ao usuário.

