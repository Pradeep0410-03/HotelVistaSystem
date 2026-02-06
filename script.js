 /* =======================
    Nav section
  ======================= */
const header = document.querySelector(".header");
const hero = document.querySelector(".hero");
const toggle = document.querySelector(".nav-toggle");
const navbar = document.querySelector(".navbar");

window.addEventListener("scroll", () => {
  if (window.scrollY > 90) {
    // user left hero → hide navbar
    header.classList.add("hide");
  } else {
    // user inside hero → show navbar
    header.classList.remove("hide");
  }
});
toggle.addEventListener("click", () => {
  navbar.classList.toggle("active");
});



(function () {
  const hero = document.querySelector('#home');
  if (!hero) return;

  /* =======================
     GUEST ELEMENTS
  ======================= */
  const guestInput = hero.querySelector('input[name="guests"]');
  const panel = hero.querySelector('.guest-panel');
  const doneBtn = hero.querySelector('.guest-done');

  /* =======================
     DATE ELEMENTS
  ======================= */
  const dateInput = hero.querySelector('input[name="dates"]');
  const datePanel = hero.querySelector('.date-panel');
  const startInput = hero.querySelector('.date-start');
  const endInput = hero.querySelector('.date-end');
  const dateDone = hero.querySelector('.date-done');

  if (!guestInput || !panel || !doneBtn || !dateInput || !datePanel) return;

  /* =======================
     STATE
  ======================= */
  const state = {
    adults: 2,
    children: 0,
    rooms: 1
  };

  /* =======================
     GUEST INPUT LOGIC
  ======================= */
  guestInput.addEventListener('click', (e) => {
    e.stopPropagation();
    panel.hidden = !panel.hidden;
    datePanel.hidden = true;
  });

  panel.addEventListener('click', (e) => {
    e.stopPropagation();
  });

  panel.addEventListener('click', (e) => {
    const btn = e.target;
    if (!btn.dataset.action) return;

    const type = btn.dataset.type;
    const action = btn.dataset.action;

    if (action === 'plus') state[type]++;
    if (action === 'minus' && state[type] > 0) {
      if (type === 'adults' && state.adults === 1) return;
      if (type === 'rooms' && state.rooms === 1) return;
      state[type]--;
    }

    panel.querySelector(`[data-value="${type}"]`).textContent = state[type];
    updateGuestInput();
  });

  function updateGuestInput() {
    guestInput.value =
      `${state.adults} adult${state.adults > 1 ? 's' : ''} · ` +
      `${state.children} children · ` +
      `${state.rooms} room${state.rooms > 1 ? 's' : ''}`;
  }

  doneBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    panel.hidden = true;
  });

  /* =======================
     DATE INPUT LOGIC
  ======================= */
  dateInput.addEventListener('click', (e) => {
    e.stopPropagation();
    datePanel.hidden = !datePanel.hidden;
    panel.hidden = true;
  });

  datePanel.addEventListener('click', (e) => {
    e.stopPropagation();
  });

  dateDone.addEventListener('click', () => {
    if (!startInput.value || !endInput.value) return;

    const start = new Date(startInput.value);
    const end = new Date(endInput.value);

    if (end < start) {
      alert('Check-out must be after check-in');
      return;
    }

    const options = { day: '2-digit', month: 'short', year: 'numeric' };

    dateInput.value =
      `${start.toLocaleDateString('en-GB', options)} — ` +
      `${end.toLocaleDateString('en-GB', options)}`;

    datePanel.hidden = true;
  });

  /* =======================
     CLOSE ON OUTSIDE CLICK
  ======================= */
  document.addEventListener('click', () => {
    panel.hidden = true;
    datePanel.hidden = true;
  });

})();
//cateogry//

const tabs = document.querySelectorAll(".inspiration-tabs li");
const items = document.querySelectorAll(".inspiration-item");

tabs.forEach(tab => {
  tab.addEventListener("click", () => {
    // active tab UI
    tabs.forEach(t => t.classList.remove("active"));
    tab.classList.add("active");

    const category = tab.dataset.category;

    items.forEach(item => {
      // always show "Show more"
      if (item.classList.contains("show-more")) {
        item.style.display = "block";
        return;
      }

      if (category === "all" || item.dataset.category === category) {
        item.style.display = "block";
      } else {
        item.style.display = "none";
      }
    });
  });
});
