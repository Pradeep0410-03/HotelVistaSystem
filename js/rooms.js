/* ===============================
   ROOMS SECTION LOGIC
================================ */

// Get all room sliders (one per city)
const roomSliders = document.querySelectorAll(".rooms-container");

// Exit if no rooms section on page
if (roomSliders.length) {
  roomSliders.forEach(slider => {
    let isDown = false;
    let startX;
    let scrollLeft;

    // Mouse events (desktop)
    slider.addEventListener("mousedown", e => {
      isDown = true;
      slider.classList.add("is-dragging");
      startX = e.pageX - slider.offsetLeft;
      scrollLeft = slider.scrollLeft;
    });

    slider.addEventListener("mouseleave", () => {
      isDown = false;
      slider.classList.remove("is-dragging");
    });

    slider.addEventListener("mouseup", () => {
      isDown = false;
      slider.classList.remove("is-dragging");
    });

    slider.addEventListener("mousemove", e => {
      if (!isDown) return;
      e.preventDefault();
      const x = e.pageX - slider.offsetLeft;
      const walk = (x - startX) * 1.5; // scroll speed
      slider.scrollLeft = scrollLeft - walk;
    });
  });
}

/* ===============================
   ROOM CARD ACTIVE STATE
================================ */

const roomCards = document.querySelectorAll(".room-card");

roomCards.forEach(card => {
  // Desktop hover
  card.addEventListener("mouseenter", () => {
    card.classList.add("active");
  });

  card.addEventListener("mouseleave", () => {
    card.classList.remove("active");
  });

  // Mobile touch feedback
  card.addEventListener("touchstart", () => {
    card.classList.add("active");
  });

  card.addEventListener("touchend", () => {
    card.classList.remove("active");
  });
});
