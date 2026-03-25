'use strict';

// HP-41C RPN Calculator Engine
const calc = (() => {
  // Stack: X (display), Y, Z, T
  let stack = { x: 0, y: 0, z: 0, t: 0 };
  let lastX = 0;
  let memory = new Array(10).fill(0);

  // Input state
  let inputStr = '';      // current digit string being entered
  let inputMode = false;  // true when user is typing digits
  let enterPressed = false; // lift stack on next digit?
  let eexMode = false;    // entering exponent?
  let eexStr = '';
  let shifted = false;
  let angleMode = 'DEG'; // DEG | RAD | GRAD
  let stoWaiting = false;
  let rclWaiting = false;

  // ---- Display formatting ----
  function formatNum(n) {
    if (!isFinite(n)) return isNaN(n) ? ' NaN' : (n > 0 ? ' 9.9999E99' : '-9.9999E99');
    if (n === 0) return '0.0000';
    const abs = Math.abs(n);
    if (abs >= 1e10 || (abs < 1e-4 && abs > 0)) {
      // Scientific notation
      let s = n.toExponential(4);
      // Normalize to HP style: e.g. 1.2345E-7
      return s.replace('e+', 'E').replace('e-', 'E-').replace('e', 'E').toUpperCase();
    }
    // Fixed: show up to 4 decimal places, trim trailing zeros (min 1)
    let fixed = n.toFixed(4);
    // Remove unnecessary trailing zeros but keep at least one decimal
    fixed = fixed.replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '.0');
    return fixed;
  }

  function updateDisplay() {
    const dispEl = document.getElementById('display');
    const regY = document.getElementById('reg-y');
    const regZ = document.getElementById('reg-z');
    const regT = document.getElementById('reg-t');

    if (inputMode) {
      let shown = inputStr;
      if (eexMode) shown += 'E' + eexStr;
      dispEl.textContent = shown || '0';
    } else {
      dispEl.textContent = formatNum(stack.x);
    }

    regY.textContent = formatNum(stack.y);
    regZ.textContent = formatNum(stack.z);
    regT.textContent = formatNum(stack.t);

    // Annunciators
    document.getElementById('ann-shift').classList.toggle('active', shifted);
    document.getElementById('ann-alpha').classList.toggle('active', angleMode === 'RAD');
    document.getElementById('ann-prgm').classList.toggle('active', angleMode === 'GRAD');
  }

  function flashError(msg) {
    const dispEl = document.getElementById('display');
    dispEl.textContent = msg || 'ERROR';
    dispEl.classList.add('error');
    setTimeout(() => {
      dispEl.classList.remove('error');
      updateDisplay();
    }, 800);
  }

  // ---- Stack operations ----
  function liftStack() {
    stack.t = stack.z;
    stack.z = stack.y;
    stack.y = stack.x;
  }

  function dropStack() {
    stack.x = stack.y;
    stack.y = stack.z;
    stack.z = stack.t;
    // T replicates (HP behavior)
  }

  function commitInput() {
    if (!inputMode) return;
    let val;
    if (eexMode) {
      const base = parseFloat(inputStr || '0');
      const exp = parseInt(eexStr || '0', 10);
      val = base * Math.pow(10, exp);
    } else {
      val = parseFloat(inputStr || '0');
    }
    stack.x = isNaN(val) ? 0 : val;
    inputMode = false;
    eexMode = false;
    inputStr = '';
    eexStr = '';
  }

  function toRad(deg) {
    if (angleMode === 'RAD') return deg;
    if (angleMode === 'GRAD') return deg * Math.PI / 200;
    return deg * Math.PI / 180;
  }

  function fromRad(rad) {
    if (angleMode === 'RAD') return rad;
    if (angleMode === 'GRAD') return rad * 200 / Math.PI;
    return rad * 180 / Math.PI;
  }

  // ---- Key handlers ----
  function handleDigit(d) {
    if (stoWaiting || rclWaiting) {
      const reg = parseInt(d, 10);
      if (stoWaiting) {
        commitInput();
        memory[reg] = stack.x;
        stoWaiting = false;
      } else {
        if (!inputMode && enterPressed) { liftStack(); }
        stack.x = memory[reg];
        enterPressed = false;
        rclWaiting = false;
      }
      updateDisplay();
      return;
    }

    if (!inputMode) {
      if (enterPressed) {
        liftStack();
        enterPressed = false;
      }
      inputStr = '';
      eexMode = false;
      eexStr = '';
      inputMode = true;
    }

    if (eexMode) {
      if (eexStr.replace('-', '').length < 2) {
        eexStr += d;
      }
    } else {
      if (inputStr.replace('-', '').replace('.', '').length < 10) {
        inputStr += d;
      }
    }
    updateDisplay();
  }

  function handleDot() {
    if (!inputMode) {
      if (enterPressed) { liftStack(); enterPressed = false; }
      inputStr = '0';
      inputMode = true;
    }
    if (eexMode) return;
    if (!inputStr.includes('.')) {
      inputStr += '.';
    }
    updateDisplay();
  }

  function handleEnter() {
    commitInput();
    liftStack();
    // X stays the same; mark that next digit should NOT lift again
    enterPressed = true;
    updateDisplay();
  }

  function handleCHS() {
    if (inputMode) {
      if (eexMode) {
        eexStr = eexStr.startsWith('-') ? eexStr.slice(1) : '-' + eexStr;
      } else {
        inputStr = inputStr.startsWith('-') ? inputStr.slice(1) : '-' + inputStr;
      }
    } else {
      stack.x = -stack.x;
    }
    updateDisplay();
  }

  function handleEEX() {
    if (!inputMode) {
      if (enterPressed) { liftStack(); enterPressed = false; }
      inputStr = '1';
      inputMode = true;
    }
    if (!eexMode) {
      eexMode = true;
      eexStr = '';
    }
    updateDisplay();
  }

  function handleCLX() {
    inputMode = false;
    inputStr = '';
    eexMode = false;
    eexStr = '';
    stack.x = 0;
    enterPressed = false;
    updateDisplay();
  }

  function handleArith(op) {
    commitInput();
    const x = stack.x;
    const y = stack.y;
    lastX = x;
    let result;
    switch (op) {
      case 'add': result = y + x; break;
      case 'sub': result = y - x; break;
      case 'mul': result = y * x; break;
      case 'div':
        if (x === 0) { flashError('DIV BY 0'); return; }
        result = y / x;
        break;
    }
    stack.x = result;
    stack.y = stack.z;
    stack.z = stack.t;
    enterPressed = false;
    updateDisplay();
  }

  function handleUnary(fn) {
    commitInput();
    lastX = stack.x;
    const x = stack.x;
    let result;
    try {
      switch (fn) {
        case 'sqrt':
          if (x < 0) { flashError('SQRT NEG'); return; }
          result = Math.sqrt(x); break;
        case 'inv':
          if (x === 0) { flashError('DIV BY 0'); return; }
          result = 1 / x; break;
        case 'log':
          if (x <= 0) { flashError('LOG NEG'); return; }
          result = Math.log10(x); break;
        case 'ln':
          if (x <= 0) { flashError('LN NEG'); return; }
          result = Math.log(x); break;
        case 'sin': result = Math.sin(toRad(x)); break;
        case 'cos': result = Math.cos(toRad(x)); break;
        case 'tan':
          result = Math.tan(toRad(x));
          if (!isFinite(result)) { flashError('TAN UNDEF'); return; }
          break;
        case 'asin':
          if (x < -1 || x > 1) { flashError('ASIN ERR'); return; }
          result = fromRad(Math.asin(x)); break;
        case 'acos':
          if (x < -1 || x > 1) { flashError('ACOS ERR'); return; }
          result = fromRad(Math.acos(x)); break;
        case 'atan': result = fromRad(Math.atan(x)); break;
        case 'exp': result = Math.exp(x); break;
        case 'pow10': result = Math.pow(10, x); break;
        case 'x2': result = x * x; break;
        case 'sigma':
          // Simple: push x^2 to Y, x stays (statistical accumulation stub)
          liftStack();
          stack.x = x;
          updateDisplay();
          return;
        default: return;
      }
    } catch (e) {
      flashError('ERROR');
      return;
    }
    stack.x = result;
    enterPressed = false;
    updateDisplay();
  }

  function handleXY() {
    commitInput();
    const tmp = stack.x;
    stack.x = stack.y;
    stack.y = tmp;
    updateDisplay();
  }

  function handleRoll() {
    commitInput();
    const tmp = stack.x;
    stack.x = stack.y;
    stack.y = stack.z;
    stack.z = stack.t;
    stack.t = tmp;
    updateDisplay();
  }

  function handleLastX() {
    liftStack();
    stack.x = lastX;
    updateDisplay();
  }

  function handleSTO() {
    stoWaiting = true;
    rclWaiting = false;
    updateDisplay();
  }

  function handleRCL() {
    rclWaiting = true;
    stoWaiting = false;
    updateDisplay();
  }

  function handleMode() {
    const modes = ['DEG', 'RAD', 'GRAD'];
    const idx = modes.indexOf(angleMode);
    angleMode = modes[(idx + 1) % 3];
    document.getElementById('key-mode').textContent = angleMode;
    updateDisplay();
  }

  function handleShift() {
    shifted = !shifted;
    updateDisplay();
  }

  function handleShiftedAction(base) {
    // Shifted functions map
    const shiftMap = {
      'sqrt': 'x2',
      'log': 'pow10',
      'ln': 'exp',
      'sin': 'asin',
      'cos': 'acos',
      'tan': 'atan',
      'inv': 'sigma',
      'xy': 'lastx',
      'roll': 'clear',
      'on': 'off',
    };
    const mapped = shiftMap[base];
    shifted = false;
    if (mapped === 'lastx') { handleLastX(); return; }
    if (mapped === 'clear') { handleAllClear(); return; }
    if (mapped === 'off') { return; } // no-op in web app
    if (mapped) handleUnary(mapped);
    else handleAction(base);
    updateDisplay();
  }

  function handleAllClear() {
    stack = { x: 0, y: 0, z: 0, t: 0 };
    lastX = 0;
    inputMode = false;
    inputStr = '';
    eexMode = false;
    eexStr = '';
    enterPressed = false;
    stoWaiting = false;
    rclWaiting = false;
    updateDisplay();
  }

  function handleAction(action, extra) {
    // Cancel STO/RCL waiting if non-digit key pressed
    if (stoWaiting || rclWaiting) {
      if (action !== 'digit') {
        stoWaiting = false;
        rclWaiting = false;
      }
    }

    if (shifted && action !== 'shift' && action !== 'on') {
      shifted = false;
      handleShiftedAction(action);
      updateDisplay();
      return;
    }

    switch (action) {
      case 'digit':   handleDigit(extra); break;
      case 'dot':     handleDot(); break;
      case 'enter':   handleEnter(); break;
      case 'chs':     handleCHS(); break;
      case 'eex':     handleEEX(); break;
      case 'clx':     handleCLX(); break;
      case 'op':      handleArith(extra); break;
      case 'sqrt':    handleUnary('sqrt'); break;
      case 'inv':     handleUnary('inv'); break;
      case 'log':     handleUnary('log'); break;
      case 'ln':      handleUnary('ln'); break;
      case 'sin':     handleUnary('sin'); break;
      case 'cos':     handleUnary('cos'); break;
      case 'tan':     handleUnary('tan'); break;
      case 'xy':      handleXY(); break;
      case 'roll':    handleRoll(); break;
      case 'sto':     handleSTO(); break;
      case 'rcl':     handleRCL(); break;
      case 'mode':    handleMode(); break;
      case 'shift':   handleShift(); break;
      case 'sigma':   handleUnary('sigma'); break;
      case 'on':      handleAllClear(); break;
    }
  }

  return { handleAction, updateDisplay };
})();

// ---- Event Binding ----
document.querySelectorAll('.key').forEach(btn => {
  btn.addEventListener('click', () => {
    const action = btn.dataset.action;
    const digit  = btn.dataset.digit;
    const op     = btn.dataset.op;
    btn.classList.add('pressed');
    setTimeout(() => btn.classList.remove('pressed'), 120);
    if (action === 'digit') calc.handleAction('digit', digit);
    else if (action === 'op') calc.handleAction('op', op);
    else calc.handleAction(action);
  });
});

// Keyboard support
document.addEventListener('keydown', e => {
  const k = e.key;
  if (k >= '0' && k <= '9') { calc.handleAction('digit', k); return; }
  switch (k) {
    case '.': case ',': calc.handleAction('dot'); break;
    case 'Enter': calc.handleAction('enter'); break;
    case '+': calc.handleAction('op', 'add'); break;
    case '-': calc.handleAction('op', 'sub'); break;
    case '*': calc.handleAction('op', 'mul'); break;
    case '/': e.preventDefault(); calc.handleAction('op', 'div'); break;
    case 'Backspace': calc.handleAction('clx'); break;
    case 'Delete': calc.handleAction('on'); break;
    case 's': calc.handleAction('shift'); break;
  }
});

// Init
calc.updateDisplay();
