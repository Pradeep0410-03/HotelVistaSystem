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

