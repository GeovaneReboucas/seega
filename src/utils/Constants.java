package src.utils;

public class Constants {
    // Configurações de conexão
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 12345;

    // Configurações do tabuleiro
    public static final int BOARD_SIZE = 5;
    public static final int CENTER_ROW = 2;
    public static final int CENTER_COL = 2;

    // Símbolos dos jogadores
    public static final String PLAYER_1_SYMBOL = "O";
    public static final String PLAYER_2_SYMBOL = "X";

    // Fases do jogo
    public static final int PLACEMENT_PHASE_END_TURN = 24;
    public static final int MOVEMENT_PHASE_START_TURN = 25;

    // Configurações de jogo
    public static final int MOVES_PER_BLOCK = 2;
    public static final int INITIAL_TURN = 1;

    // Prefixos de mensagem
    public static final String ID_PREFIX = "ID:";
    public static final String TURN_PREFIX = "TURN:";
    public static final String STARTING_PLAYER_PREFIX = "STARTINGPLAYER:";
    public static final String AUTO_PASS_PREFIX = "AUTOPASS:";
    public static final String GAME_OVER_PREFIX = "GAMEOVER:";
    public static final String MOVE_PREFIX = "MOVE:";
    public static final String MOVE_PIECE_PREFIX = "MOVEPIECE:";
    public static final String CAPTURE_PREFIX = "CAPTURE:";
    public static final String CENTER_PREFIX = "CENTER:";

    // Configurações do jogo
    public static final int CENTER_UNLOCK_TURN = 25;

    // Mensagens de erro
    public static final String CONNECTION_ERROR_TITLE = "Erro de Conexão";
    public static final String CONNECTION_ERROR_MESSAGE = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando.";
    public static final String DISCONNECTION_MESSAGE = "Conexão com o servidor perdida.";

    // Mensagens de diálogo
    public static final String ENTER_VALID_PORT = "Por favor, insira uma porta valida.";
    public static final String STARTING_PLAYER_TITLE = "Escolha o jogador inicial";
    public static final String STARTING_PLAYER_QUESTION = "Qual jogador deve iniciar?";
    public static final String[] PLAYER_OPTIONS = { "Jogador 1", "Jogador 2" };
    public static final String DEFAULT_PLAYER_OPTION = "Jogador 1";

    // Mensagens de jogo
    public static final String AUTO_PASS_MESSAGE = " não tem movimentos válidos. Turno passado automaticamente.";

    // Mensagens do servidor
    public static final String SERVER_START_MESSAGE = "Servidor iniciado na porta ";
    public static final String CLIENT_DISCONNECT_MESSAGE = "Cliente %d desconectado.";
    public static final String RESIGN_MESSAGE = "Jogador %d desistiu da partida!";
    public static final String DISCONNECT_MESSAGE = "Jogador %d desconectou!";

}