(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const api = window.HotelVistaProperties.createClient(window.fetch.bind(window));
  let city = new URLSearchParams(location.search).get('city') || '', page = 0, hasNext = false, requestNumber = 0, controller;
  $('database-city').value = city.slice(0,100); city = $('database-city').value;
  function element(tag, text, className) {
    const node = document.createElement(tag); node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  function card(property) {
    const article = element('article', '', 'database-card');
    article.append(element('span', property.propertyType.replaceAll('_',' '), 'eyebrow dark'), element('h2', property.name), element('p', property.city), element('p', property.address));
    const detail = element('div', ''); detail.hidden = true;
    const button = element('button', 'View details', 'text-button'); button.type = 'button'; button.setAttribute('aria-expanded','false');
    button.addEventListener('click', async () => {
      if (!detail.hidden) { detail.hidden = true; button.setAttribute('aria-expanded','false'); button.textContent = 'View details'; return; }
      button.disabled = true;
      try {
        const value = await api.detail(property.id, AbortSignal.timeout(15000));
        detail.replaceChildren(element('p',value.description || 'No description has been added yet.'),element('p','Local timezone: ' + value.timezone),element('p','Rooms and booking are not available yet.'));
        detail.hidden = false; button.setAttribute('aria-expanded','true'); button.textContent = 'Hide details';
      } catch(error) {
        detail.replaceChildren(element('p',error.message || 'Details could not be loaded.'));
        detail.hidden = false; button.setAttribute('aria-expanded','true'); button.textContent = 'Hide message';
      } finally { button.disabled = false; }
    });
    article.append(button,detail); return article;
  }
  async function load(targetPage = 0) {
    const number = ++requestNumber;
    if (controller) controller.abort(); controller = new AbortController();
    const activeController = controller;
    const timer = setTimeout(() => activeController.abort(),15000);
    $('database-grid').replaceChildren(); $('database-grid').setAttribute('aria-busy','true');
    $('database-status').textContent = 'Loading properties…'; $('database-retry').hidden = true;
    $('database-prev').disabled = true; $('database-next').disabled = true; $('database-page').textContent = '';
    try {
      const result = await api.list(city, targetPage, activeController.signal);
      if (number !== requestNumber) return;
      page = targetPage; hasNext = result.hasNext;
      $('database-grid').replaceChildren(...result.items.map(card));
      $('database-status').textContent = result.items.length ? result.items.length + ' listed properties' + (city ? ' in ' + city : '') + '.' : 'No properties found' + (city ? ' in ' + city : '') + '. Try another city or check back later.';
      $('database-page').textContent = 'Page ' + (page+1);
      $('database-prev').disabled = page === 0;
      $('database-next').disabled = !hasNext || page >= 10000;
    } catch(error) {
      if (number !== requestNumber) return;
      $('database-status').textContent = error.name === 'AbortError' ? 'The request timed out. Please try again.' : error.message || 'Properties could not be loaded.';
      $('database-retry').hidden = false;
    } finally {
      clearTimeout(timer);
      if (number === requestNumber) $('database-grid').setAttribute('aria-busy','false');
    }
  }
  function mode(samples) {
    $('sample-catalogue').hidden = !samples; $('sample-navigation').hidden = !samples; $('database-browse').hidden = samples;
    $('show-database').setAttribute('aria-pressed',String(!samples)); $('show-samples').setAttribute('aria-pressed',String(samples));
    if (!samples) load(0);
  }
  $('show-database').addEventListener('click', () => mode(false));
  $('show-samples').addEventListener('click', () => mode(true));
  $('database-search').addEventListener('submit', event => { event.preventDefault(); city = $('database-city').value.trim(); load(0); });
  $('database-clear').addEventListener('click', () => { city = ''; $('database-city').value = ''; load(0); });
  $('database-retry').addEventListener('click', () => load(0));
  $('database-prev').addEventListener('click', () => { if (page > 0) load(page-1); });
  $('database-next').addEventListener('click', () => { if (hasNext) load(page+1); });
  const params = new URLSearchParams(location.search);
  mode(params.has('property') || params.has('destination') || params.get('catalogue') === 'sample');
})();
