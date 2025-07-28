# Sistema de Gerenciamento e Utilização de Comunicação por Mensagens

Este projeto implementa um sistema completo de chat utilizando Java e ActiveMQ, incluindo uma interface de administração para gerenciamento do broker.
## Como Executar

### Pré-requisitos
- Java 11 ou superior (JDK)
- Windows 7 ou superior
- ActiveMQ 5.19.0 (incluído no projeto)

### Passos para Execução no Windows

1. **Compilar o projeto**:
   ```cmd
   compile.bat
   ```

2. **Iniciar o ActiveMQ**:
   ```cmd
   start-activemq.bat
   ```
   - Console web disponível em: http://localhost:8161/admin
   - Usuário: admin, Senha: admin

3. **Executar o servidor de chat**:
   ```cmd
   run-server.bat
   ```

4. **Executar a interface de administração**:
   ```cmd
   run-admin.bat
   ```

5. **Executar clientes de chat** (opcional, para testes):
   ```cmd
   run-client.bat
   ```
