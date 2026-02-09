document.querySelectorAll('.scroll-wrapper').forEach(wrapper => {
  const container = wrapper.querySelector('.scroll-container');
  const btnLeft = wrapper.querySelector('.scroll-btn.left');
  const btnRight = wrapper.querySelector('.scroll-btn.right');

  const scrollAmount = 320;

  btnLeft.addEventListener('click', () => {
    container.scrollBy({
      left: -scrollAmount,
      behavior: 'smooth'
    });
  });

  btnRight.addEventListener('click', () => {
    container.scrollBy({
      left: scrollAmount,
      behavior: 'smooth'
    });
  });

  /* Hide / show arrows dynamically */
  const updateButtons = () => {
    btnLeft.style.display =
      container.scrollLeft <= 0 ? 'none' : 'flex';

    btnRight.style.display =
      container.scrollLeft + container.clientWidth >= container.scrollWidth
        ? 'none'
        : 'flex';
  };

  container.addEventListener('scroll', updateButtons);
  window.addEventListener('load', updateButtons);
});
