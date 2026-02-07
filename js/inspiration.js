const tabs = document.querySelectorAll(".inspiration-tabs li");
const items = document.querySelectorAll(".inspiration-item");

tabs.forEach(tab => {
  tab.addEventListener("click", () => {
    tabs.forEach(t => t.classList.remove("active"));
    tab.classList.add("active");

    const category = tab.dataset.category;

    items.forEach(item => {
      if (item.classList.contains("show-more")) {
        item.classList.remove("hidden");
        return;
      }

      if (category === "all" || item.dataset.category === category) {
        item.classList.remove("hidden");
      } else {
        item.classList.add("hidden");
      }
    });
  });
});
