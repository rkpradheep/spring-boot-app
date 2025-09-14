class KeyCodes {
  static get LEFT_ARROW() { return 37; }
  static get UP_ARROW() { return 38; }
  static get RIGHT_ARROW() { return 39; }
  static get DOWN_ARROW() { return 40; }
}

const BG_COLOUR = '#000';
const FOOD_COLOUR = '#e66916';
var prevKeyCode = KeyCodes.RIGHT_ARROW;
var socket;
const gameContainer = document.getElementById('game-container');
const gameScreen = document.getElementById('game-screen');
const initialScreen = document.getElementById('initial-screen');
const createGameButton = document.getElementById('create-game');
const joinGameNow = document.getElementById('join-game');
const joinGameInput = document.getElementById('join-game-code');
const gameCodeDisplay = document.getElementById('game-code');
const joinGameButton = document.getElementById('join-game-button');
const joinGameContainer = document.getElementById('join-game-container');
const resultBox = document.getElementById('result-box');
createGameButton.addEventListener('click', newGame);
joinGameNow.addEventListener('click', joinGame);


joinGameButton.addEventListener('click', () => {
        joinGameContainer.style.display = 'flex';
});

function establishConnection(gameCode)
{
    const name = prompt('Enter your name')
    if(!name)
    {
        alert('Invalid name!')
        return
    }
    unHideElement('loading')
    var url = "ws://";
    if (window.location.origin.includes('https'))
        url = "wss://";
    url = url + window.location.host + "/snakegame?game_code=" + gameCode + "&name=" + name;
    socket = new WebSocket(url);

    socket.addEventListener('open', function(event) {
        console.log("session started")
    });


    socket.onmessage = (msg) => {
    let req = JSON.parse(msg.data)

   // console.log(req)

    if(req.command == 'init')
        handleInit(req.data)
    if(req.command == 'gameState')
        handleGameState(req.data)
    if(req.command == 'gameOver')
        handleGameOver(req.data)
    if(req.command == 'gameCode')
        handleGameCode(req.data)
    if(req.command == 'unknownCode')
        handleUnknownCode()
    if(req.command == 'tooManyPlayers')
        handleTooManyPlayers()
    if(req.command == 'showResult')
        handleResults(req.data)
    };
    socket.addEventListener("close", function(event) {
        hideElement('loading')
        console.log("WebSocket Closed:");
        console.log(event)
        reset()
    });


}

function newGame() {
 establishConnection('')
}

function joinGame() {
  const code = joinGameInput.value;
  if(!code)
  {
    alert("Invalid code!")
    return
  }

  establishConnection(code)
}

let canvas, ctx;
let playerNumber;
let gameActive = false;

function init() {
  initialScreen.style.display = "none";
  gameContainer.style.display = "flex";

  canvas = document.getElementById('canvas');
  ctx = canvas.getContext('2d');

  canvas.width = gameScreen.clientWidth;
  canvas.height = gameScreen.clientHeight;

  ctx.fillStyle = BG_COLOUR;
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  document.addEventListener('keydown', keydown);
  gameActive = true;
}

function keydown(e) {
 moveSnake(e.keyCode);
}

function moveSnake(keyCode)
{
if(keyCode == KeyCodes.LEFT_ARROW || keyCode == KeyCodes.RIGHT_ARROW || keyCode == KeyCodes.UP_ARROW | keyCode == KeyCodes.DOWN_ARROW)
{
  if((prevKeyCode == KeyCodes.LEFT_ARROW && keyCode == KeyCodes.RIGHT_ARROW) || (prevKeyCode == KeyCodes.RIGHT_ARROW && keyCode == KeyCodes.LEFT_ARROW))
  {
    return;
  }

  if((prevKeyCode == KeyCodes.UP_ARROW && keyCode == KeyCodes.DOWN_ARROW) || (prevKeyCode == KeyCodes.DOWN_ARROW && keyCode == KeyCodes.UP_ARROW))
  {
    return;
  }

  prevKeyCode = keyCode

  const data = {
  'command' : 'keydown',
  'data' : {'key_code' : keyCode}
  }
  socket.send(JSON.stringify(data));
 }
}

function paintGame(state) {
  ctx.fillStyle = BG_COLOUR;
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  const food = state.food;
  const gridsize = state.gridsize;
  const xAxisSize = canvas.width / gridsize;
  const yAxisSize = canvas.height / gridsize;

  ctx.fillStyle = FOOD_COLOUR;
  ctx.fillRect(food.x * xAxisSize, food.y * yAxisSize, xAxisSize, yAxisSize);

  paintPlayer(state.players[0], xAxisSize, yAxisSize, state.players[0].color);
  for(var i=1 ; i < state.players.length ; i++)
  paintPlayer(state.players[i], xAxisSize, yAxisSize, state.players[i].color);

  updateResultBox(state.players)
}

function paintPlayer(playerState, xAxisSize, yAxisSize, colour) {

  if(playerState.eliminated == true)
  {
    return;
  }
  const snake = playerState.snake;
  var i = 0;
  for (let cell of snake) {
    if(i == 0){
     ctx.font = '16px Arial';
     ctx.fillStyle = 'white';
     if(prevKeyCode == KeyCodes.RIGHT_ARROW)
     ctx.fillText(playerState.name, (cell.x * xAxisSize) - 20, (cell.y * yAxisSize) + 50);
     if(prevKeyCode == KeyCodes.LEFT_ARROW)
     ctx.fillText(playerState.name, (cell.x * xAxisSize) + 10, (cell.y * yAxisSize) + 50);
     if(prevKeyCode == KeyCodes.DOWN_ARROW)
     ctx.fillText(playerState.name, (cell.x * xAxisSize) - 10, (cell.y * yAxisSize) + 50);
     if(prevKeyCode == KeyCodes.UP_ARROW)
     ctx.fillText(playerState.name, (cell.x * xAxisSize) - 10, (cell.y * yAxisSize) - 20);

     var eyeRadius = xAxisSize / 10;
     var eyeOffsetX = xAxisSize / 4;
     var eyeOffsetY = yAxisSize / 4;
     }

    ctx.fillStyle = colour;
    ctx.fillRect(cell.x * xAxisSize, cell.y * yAxisSize, xAxisSize, yAxisSize);

    if(i == 0)
    {
         // Draw the left eye
         ctx.beginPath();
         ctx.arc((cell.x * xAxisSize) + eyeOffsetX, (cell.y * yAxisSize) + eyeOffsetY, eyeRadius, 0, Math.PI * 2, true);
         ctx.fillStyle = "black";
         ctx.fill();

         // Draw the right eye
         ctx.beginPath();
         ctx.arc((cell.x * xAxisSize) + xAxisSize - eyeOffsetX, (cell.y * yAxisSize) + eyeOffsetY, eyeRadius, 0, Math.PI * 2, true);
         ctx.fillStyle = "black";
         ctx.fill();
    }

    i = i+1;
  }
}

function handleInit(data) {
  init();
  playerNumber = data.number;
  hideElement('loading')
}

function handleGameState(gameState) {
  if (!gameActive) {
    return;
  }
  requestAnimationFrame(() => paintGame(gameState));
}

function handleGameOver(data) {
  if (!gameActive) {
    return;
  }
    alert('You lost!');
}


function handleResults(data) {
  if (!gameActive) {
    return;
  }
    alert("Winner is " + data.winner);
    window.location.reload();
}

function handleGameCode(data) {
  gameCodeDisplay.innerHTML = "Your game code is: " + data.game_code;
}

function handleUnknownCode() {
  reset();
  alert('Unknown Game Code')
}

function handleTooManyPlayers() {
  reset();
  alert('Player limit exceeded');
}

function reset() {
  playerNumber = null;
  gameCodeDisplay.innerHTML = '';
  initialScreen.style.display = "flex";
  gameContainer.style.display = "none";

}

function updateResultBox(players) {
        resultBox.innerHTML = players.map(player => `<strong>${player.name}</strong>: ${player.points}`).join('<br>');
}

document.getElementById('up').addEventListener('click', () => { moveSnake(KeyCodes.UP_ARROW); });
document.getElementById('left').addEventListener('click', () => { moveSnake(KeyCodes.LEFT_ARROW); });
document.getElementById('down').addEventListener('click', () => { moveSnake(KeyCodes.DOWN_ARROW); });
document.getElementById('right').addEventListener('click', () => { moveSnake(KeyCodes.RIGHT_ARROW); });