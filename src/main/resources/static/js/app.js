'use strict';

/* =====================================================================
   Front-end da Locadora — HTML/CSS/JS puros, consumindo a API REST em /api
   ===================================================================== */

// ------------------------------------------------------------ utilitários

const $ = (sel, el = document) => el.querySelector(sel);
const $$ = (sel, el = document) => [...el.querySelectorAll(sel)];

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const money = (v) => (v == null ? '—' : brl.format(v));
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const dataBR = (iso) => (iso ? iso.split('-').reverse().join('/') : '—');
const cpfMask = (c) => (c ? c.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4') : '');

/** Data local no formato yyyy-MM-dd (com deslocamento opcional em dias). */
function hojeISO(offset = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function diffDias(a, b) {
  if (!a || !b) return null;
  const [y1, m1, d1] = a.split('-').map(Number);
  const [y2, m2, d2] = b.split('-').map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

const numOuNull = (v) => (v === '' || v == null ? null : Number(v));
const textoOuNull = (v) => (v == null || v.trim() === '' ? null : v.trim());

// ------------------------------------------------------------ chamadas à API

class ApiError extends Error {
  constructor(status, body) {
    super(body && body.mensagem ? body.mensagem : `Erro ${status}`);
    this.status = status;
    this.body = body;
  }
}

async function api(path, { method = 'GET', body } = {}) {
  let res;
  try {
    res = await fetch('/api' + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw new ApiError(0, { mensagem: 'Não foi possível conectar à API. Ela está em execução?' });
  }
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch (_) { /* corpo vazio */ }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

function qs(params) {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v != null) p.append(k, v);
  });
  const s = p.toString();
  return s ? '?' + s : '';
}

// ------------------------------------------------------------ feedback ao usuário

function toast(msg, tipo = 'ok') {
  const el = document.createElement('div');
  el.className = 'toast' + (tipo === 'error' ? ' error' : '');
  el.textContent = msg;
  $('#toasts').appendChild(el);
  setTimeout(() => el.remove(), 4500);
}

function erroTexto(e) {
  let t = e.message || 'Erro inesperado';
  if (e.body && e.body.campos && e.body.campos.length) {
    t += ' — ' + e.body.campos.map((c) => `${c.campo}: ${c.mensagem}`).join('; ');
  }
  return t;
}

function mostrarErro(form, e) {
  const box = $('.form-error', form);
  let html = `<strong>${esc(e.message)}</strong>`;
  if (e.status) html += ` <span style="opacity:.7">(HTTP ${e.status})</span>`;
  if (e.body && e.body.campos && e.body.campos.length) {
    html += '<ul>' + e.body.campos
      .map((c) => `<li><b>${esc(c.campo)}</b>: ${esc(c.mensagem)}</li>`).join('') + '</ul>';
  }
  box.innerHTML = html;
  box.hidden = false;
}

function limparErro(form) {
  const box = $('.form-error', form);
  box.hidden = true;
  box.innerHTML = '';
}

// ------------------------------------------------------------ componentes

const BADGE_VEICULO = { DISPONIVEL: 'ok', ALUGADO: 'info', MANUTENCAO: 'warn' };
const BADGE_LOCACAO = { ATIVA: 'info', FINALIZADA: 'ok', CANCELADA: '' };

const badge = (texto, classe) => `<span class="badge ${classe || ''}">${esc(texto)}</span>`;

function renderPager(el, data, irPara) {
  const total = data.totalPages;
  el.innerHTML =
    `<span>${data.totalElements} registro(s) — página ${total ? data.page + 1 : 0} de ${total}</span>` +
    `<button class="btn small" data-p="prev" ${data.page <= 0 ? 'disabled' : ''}>Anterior</button>` +
    `<button class="btn small" data-p="next" ${data.page + 1 >= total ? 'disabled' : ''}>Próxima</button>`;
  el.onclick = (ev) => {
    const b = ev.target.closest('button[data-p]');
    if (!b || b.disabled) return;
    irPara(data.page + (b.dataset.p === 'next' ? 1 : -1));
  };
}

function linhaVazia(colunas, texto) {
  return `<tr><td class="empty" colspan="${colunas}">${texto}</td></tr>`;
}

function filtrosDoForm(form) {
  const out = {};
  new FormData(form).forEach((v, k) => { if (v !== '') out[k] = v; });
  return out;
}

/** Filtra ao enviar e também ao limpar (reset). */
function ligarFiltros(form, estadoRef, carregar) {
  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    estadoRef.page = 0;
    estadoRef.filtros = filtrosDoForm(form);
    carregar();
  });
  form.addEventListener('reset', () => setTimeout(() => form.requestSubmit(), 0));
}

function ligarDialogo(dlg) {
  $$('[data-close]', dlg).forEach((b) => b.addEventListener('click', () => dlg.close()));
}

// ------------------------------------------------------------ abas

const estado = {
  veiculos: { page: 0, filtros: {}, itens: [] },
  clientes: { page: 0, filtros: {}, itens: [] },
  locacoes: { page: 0, filtros: {}, itens: [] },
};

const carregadores = {
  dashboard: () => carregarDashboard(),
  veiculos: () => carregarVeiculos(),
  clientes: () => carregarClientes(),
  locacoes: () => { carregarFiltrosLocacao(); return carregarLocacoes(); },
};

function mostrarAba(nome) {
  $$('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === nome));
  $$('.panel').forEach((p) => p.classList.toggle('active', p.id === 'tab-' + nome));
  carregadores[nome]();
}

$('#tabs').addEventListener('click', (ev) => {
  const b = ev.target.closest('.tab');
  if (b) mostrarAba(b.dataset.tab);
});

// ------------------------------------------------------------ dashboard

async function carregarDashboard() {
  try {
    const d = await api('/dashboard');
    const itens = [
      ['Veículos na frota', d.totalVeiculos, ''],
      ['Disponíveis', d.veiculosDisponiveis, ''],
      ['Alugados', d.veiculosAlugados, ''],
      ['Em manutenção', d.veiculosManutencao, 'warn'],
      ['Clientes', d.totalClientes, 'muted'],
      ['Locações ativas', d.locacoesAtivas, ''],
      ['Locações atrasadas', d.locacoesAtrasadas, d.locacoesAtrasadas > 0 ? 'danger' : 'muted'],
      ['Receita (finalizadas)', money(d.receitaTotal), ''],
    ];
    $('#cards').innerHTML = itens
      .map(([l, v, c]) => `<div class="card ${c}"><div class="label">${esc(l)}</div><div class="value">${esc(v)}</div></div>`)
      .join('');
  } catch (e) {
    toast(erroTexto(e), 'error');
  }
}

// ------------------------------------------------------------ veículos

async function carregarVeiculos() {
  const st = estado.veiculos;
  try {
    const data = await api('/veiculos' + qs({ ...st.filtros, page: st.page, size: 10, sort: 'id' }));
    st.itens = data.content;
    $('#tb-veiculos').innerHTML = data.content.length
      ? data.content.map((v) => `
        <tr>
          <td><strong>${esc(v.placa)}</strong></td>
          <td>${esc(v.marca)} ${esc(v.modelo)}</td>
          <td>${v.ano}</td>
          <td>${esc(v.categoria)}</td>
          <td class="num">${money(v.valorDiaria)}</td>
          <td class="num">${v.quilometragem.toLocaleString('pt-BR')}</td>
          <td>${badge(v.status, BADGE_VEICULO[v.status])}</td>
          <td class="actions">
            <button class="btn small" data-act="editar" data-id="${v.id}">Editar</button>
            <button class="btn small danger" data-act="excluir" data-id="${v.id}">Excluir</button>
          </td>
        </tr>`).join('')
      : linhaVazia(8, 'Nenhum veículo encontrado.');
    renderPager($('#pg-veiculos'), data, (p) => { st.page = p; carregarVeiculos(); });
  } catch (e) {
    toast(erroTexto(e), 'error');
  }
}

ligarFiltros($('#f-veiculos'), estado.veiculos, carregarVeiculos);

$('#tb-veiculos').addEventListener('click', async (ev) => {
  const b = ev.target.closest('button[data-act]');
  if (!b) return;
  const id = Number(b.dataset.id);
  if (b.dataset.act === 'editar') {
    abrirVeiculo(estado.veiculos.itens.find((v) => v.id === id));
  } else if (confirm('Excluir este veículo?')) {
    try {
      await api(`/veiculos/${id}`, { method: 'DELETE' });
      toast('Veículo excluído');
      carregarVeiculos();
    } catch (e) { toast(erroTexto(e), 'error'); }
  }
});

let veiculoEditando = null;
const dlgVeiculo = $('#dlg-veiculo');
const formVeiculo = $('#form-veiculo');
ligarDialogo(dlgVeiculo);

function abrirVeiculo(v) {
  veiculoEditando = v || null;
  formVeiculo.reset();
  limparErro(formVeiculo);
  $('#dlg-veiculo-titulo').textContent = v ? `Editar veículo #${v.id}` : 'Novo veículo';
  const f = formVeiculo.elements;
  const optAlugado = $('option[value="ALUGADO"]', formVeiculo);
  const alugado = !!v && v.status === 'ALUGADO';
  optAlugado.disabled = !alugado;
  f.status.disabled = alugado;
  if (v) {
    f.placa.value = v.placa;
    f.marca.value = v.marca;
    f.modelo.value = v.modelo;
    f.ano.value = v.ano;
    f.categoria.value = v.categoria;
    f.valorDiaria.value = v.valorDiaria;
    f.quilometragem.value = v.quilometragem;
    f.status.value = v.status;
  } else {
    f.status.value = 'DISPONIVEL';
  }
  dlgVeiculo.showModal();
}

$('#novo-veiculo').addEventListener('click', () => abrirVeiculo(null));

formVeiculo.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErro(formVeiculo);
  const f = formVeiculo.elements;
  const body = {
    placa: f.placa.value.trim(),
    marca: f.marca.value.trim(),
    modelo: f.modelo.value.trim(),
    ano: numOuNull(f.ano.value),
    categoria: f.categoria.value,
    valorDiaria: numOuNull(f.valorDiaria.value),
    quilometragem: numOuNull(f.quilometragem.value),
    status: f.status.value || null,
  };
  try {
    if (veiculoEditando) {
      await api(`/veiculos/${veiculoEditando.id}`, { method: 'PUT', body });
      toast('Veículo atualizado');
    } else {
      await api('/veiculos', { method: 'POST', body });
      toast('Veículo cadastrado');
    }
    dlgVeiculo.close();
    carregarVeiculos();
  } catch (e) { mostrarErro(formVeiculo, e); }
});

// ------------------------------------------------------------ clientes

async function carregarClientes() {
  const st = estado.clientes;
  try {
    const data = await api('/clientes' + qs({ ...st.filtros, page: st.page, size: 10, sort: 'nome' }));
    st.itens = data.content;
    const hoje = hojeISO();
    $('#tb-clientes').innerHTML = data.content.length
      ? data.content.map((c) => `
        <tr>
          <td><strong>${esc(c.nome)}</strong></td>
          <td>${esc(cpfMask(c.cpf))}</td>
          <td>${esc(c.email)}</td>
          <td>${esc(c.telefone || '—')}</td>
          <td>${dataBR(c.dataNascimento)}</td>
          <td class="${c.cnhValidade < hoje ? 'expired' : ''}">${dataBR(c.cnhValidade)}${c.cnhValidade < hoje ? ' (vencida)' : ''}</td>
          <td class="actions">
            <button class="btn small" data-act="editar" data-id="${c.id}">Editar</button>
            <button class="btn small danger" data-act="excluir" data-id="${c.id}">Excluir</button>
          </td>
        </tr>`).join('')
      : linhaVazia(7, 'Nenhum cliente encontrado.');
    renderPager($('#pg-clientes'), data, (p) => { st.page = p; carregarClientes(); });
  } catch (e) {
    toast(erroTexto(e), 'error');
  }
}

ligarFiltros($('#f-clientes'), estado.clientes, carregarClientes);

$('#tb-clientes').addEventListener('click', async (ev) => {
  const b = ev.target.closest('button[data-act]');
  if (!b) return;
  const id = Number(b.dataset.id);
  if (b.dataset.act === 'editar') {
    abrirCliente(estado.clientes.itens.find((c) => c.id === id));
  } else if (confirm('Excluir este cliente?')) {
    try {
      await api(`/clientes/${id}`, { method: 'DELETE' });
      toast('Cliente excluído');
      carregarClientes();
    } catch (e) { toast(erroTexto(e), 'error'); }
  }
});

let clienteEditando = null;
const dlgCliente = $('#dlg-cliente');
const formCliente = $('#form-cliente');
ligarDialogo(dlgCliente);

function abrirCliente(c) {
  clienteEditando = c || null;
  formCliente.reset();
  limparErro(formCliente);
  $('#dlg-cliente-titulo').textContent = c ? `Editar cliente #${c.id}` : 'Novo cliente';
  if (c) {
    const f = formCliente.elements;
    f.nome.value = c.nome;
    f.cpf.value = cpfMask(c.cpf);
    f.email.value = c.email;
    f.telefone.value = c.telefone || '';
    f.dataNascimento.value = c.dataNascimento;
    f.cnhNumero.value = c.cnhNumero;
    f.cnhValidade.value = c.cnhValidade;
  }
  dlgCliente.showModal();
}

$('#novo-cliente').addEventListener('click', () => abrirCliente(null));

formCliente.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErro(formCliente);
  const f = formCliente.elements;
  const body = {
    nome: f.nome.value.trim(),
    cpf: f.cpf.value.trim(),
    email: f.email.value.trim(),
    telefone: textoOuNull(f.telefone.value),
    dataNascimento: f.dataNascimento.value || null,
    cnhNumero: f.cnhNumero.value.trim(),
    cnhValidade: f.cnhValidade.value || null,
  };
  try {
    if (clienteEditando) {
      await api(`/clientes/${clienteEditando.id}`, { method: 'PUT', body });
      toast('Cliente atualizado');
    } else {
      await api('/clientes', { method: 'POST', body });
      toast('Cliente cadastrado');
    }
    dlgCliente.close();
    carregarClientes();
  } catch (e) { mostrarErro(formCliente, e); }
});

// ------------------------------------------------------------ locações

async function carregarFiltrosLocacao() {
  try {
    const [cl, ve] = await Promise.all([
      api('/clientes?size=500&sort=nome'),
      api('/veiculos?size=500&sort=marca'),
    ]);
    const selC = $('#flt-cliente');
    const selV = $('#flt-veiculo');
    const vc = selC.value;
    const vv = selV.value;
    selC.innerHTML = '<option value="">Todos</option>' +
      cl.content.map((c) => `<option value="${c.id}">${esc(c.nome)}</option>`).join('');
    selV.innerHTML = '<option value="">Todos</option>' +
      ve.content.map((v) => `<option value="${v.id}">${esc(v.placa)} — ${esc(v.marca)} ${esc(v.modelo)}</option>`).join('');
    selC.value = vc;
    selV.value = vv;
  } catch (e) { /* os filtros continuam funcionando sem as listas */ }
}

async function carregarLocacoes() {
  const st = estado.locacoes;
  try {
    const data = await api('/locacoes' + qs({ ...st.filtros, page: st.page, size: 10 }));
    st.itens = data.content;
    const hoje = hojeISO();
    $('#tb-locacoes').innerHTML = data.content.length
      ? data.content.map((l) => {
        const ativa = l.status === 'ATIVA';
        const naoIniciada = l.dataRetirada >= hoje;
        const acoes = [];
        if (ativa) acoes.push(`<button class="btn small primary" data-act="devolver" data-id="${l.id}">Devolver</button>`);
        if (ativa && naoIniciada) {
          acoes.push(`<button class="btn small" data-act="editar" data-id="${l.id}">Editar</button>`);
          acoes.push(`<button class="btn small" data-act="cancelar" data-id="${l.id}">Cancelar</button>`);
        }
        if (!ativa) acoes.push(`<button class="btn small danger" data-act="excluir" data-id="${l.id}">Excluir</button>`);
        return `
        <tr>
          <td>${l.id}</td>
          <td>${esc(l.clienteNome)}</td>
          <td>${esc(l.veiculoDescricao)}</td>
          <td>${dataBR(l.dataRetirada)}</td>
          <td>${dataBR(l.dataPrevistaDevolucao)}</td>
          <td>${dataBR(l.dataDevolucao)}</td>
          <td class="num">${money(l.valorPrevisto)}</td>
          <td class="num">${money(l.valorFinal)}${l.multa > 0 ? ` <span title="multa/taxa" style="color:var(--warn)">(+${money(l.multa)})</span>` : ''}</td>
          <td>${badge(l.status, BADGE_LOCACAO[l.status])}${l.atrasada ? ' ' + badge('ATRASADA', 'danger') : ''}</td>
          <td class="actions">${acoes.join('')}</td>
        </tr>`;
      }).join('')
      : linhaVazia(10, 'Nenhuma locação encontrada.');
    renderPager($('#pg-locacoes'), data, (p) => { st.page = p; carregarLocacoes(); });
  } catch (e) {
    toast(erroTexto(e), 'error');
  }
}

ligarFiltros($('#f-locacoes'), estado.locacoes, carregarLocacoes);

$('#tb-locacoes').addEventListener('click', async (ev) => {
  const b = ev.target.closest('button[data-act]');
  if (!b) return;
  const id = Number(b.dataset.id);
  const loc = estado.locacoes.itens.find((l) => l.id === id);
  try {
    switch (b.dataset.act) {
      case 'devolver': abrirDevolucao(loc); break;
      case 'editar': abrirLocacao(loc); break;
      case 'cancelar':
        if (confirm('Cancelar esta locação? O veículo voltará a ficar disponível.')) {
          await api(`/locacoes/${id}/cancelar`, { method: 'PATCH' });
          toast('Locação cancelada');
          carregarLocacoes();
        }
        break;
      case 'excluir':
        if (confirm('Excluir esta locação do histórico?')) {
          await api(`/locacoes/${id}`, { method: 'DELETE' });
          toast('Locação excluída');
          carregarLocacoes();
        }
        break;
      default:
    }
  } catch (e) { toast(erroTexto(e), 'error'); }
});

// ---- diálogo de locação (criação e edição)

const dlgLocacao = $('#dlg-locacao');
const formLocacao = $('#form-locacao');
ligarDialogo(dlgLocacao);
let locacaoEditando = null;
let veiculosDisponiveis = [];

async function abrirLocacao(l) {
  locacaoEditando = l || null;
  formLocacao.reset();
  limparErro(formLocacao);
  $('#preview-locacao').textContent = '';
  $('#dlg-locacao-titulo').textContent = l ? `Editar locação #${l.id}` : 'Nova locação';
  const f = formLocacao.elements;
  try {
    if (l) {
      f.clienteId.innerHTML = `<option value="${l.clienteId}">${esc(l.clienteNome)}</option>`;
      f.veiculoId.innerHTML = `<option value="${l.veiculoId}">${esc(l.veiculoDescricao)}</option>`;
      f.clienteId.disabled = true;
      f.veiculoId.disabled = true;
      f.dataRetirada.disabled = true;
      f.dataRetirada.value = l.dataRetirada;
      f.dataPrevistaDevolucao.value = l.dataPrevistaDevolucao;
      f.observacoes.value = l.observacoes || '';
    } else {
      const [cl, ve] = await Promise.all([
        api('/clientes?size=500&sort=nome'),
        api('/veiculos?status=DISPONIVEL&size=500&sort=marca'),
      ]);
      veiculosDisponiveis = ve.content;
      f.clienteId.innerHTML = '<option value="">Selecione…</option>' +
        cl.content.map((c) => `<option value="${c.id}">${esc(c.nome)}</option>`).join('');
      f.veiculoId.innerHTML = (ve.content.length
        ? '<option value="">Selecione…</option>'
        : '<option value="">Nenhum veículo disponível</option>') +
        ve.content.map((v) =>
          `<option value="${v.id}">${esc(v.placa)} — ${esc(v.marca)} ${esc(v.modelo)} (${money(v.valorDiaria)}/dia · ${esc(v.categoria)})</option>`).join('');
      f.clienteId.disabled = false;
      f.veiculoId.disabled = false;
      f.dataRetirada.disabled = false;
      f.dataRetirada.value = hojeISO();
      f.dataPrevistaDevolucao.value = hojeISO(3);
    }
    atualizarPreview();
    dlgLocacao.showModal();
  } catch (e) { toast(erroTexto(e), 'error'); }
}

function atualizarPreview() {
  const f = formLocacao.elements;
  const box = $('#preview-locacao');
  let diaria = null;
  if (locacaoEditando) {
    diaria = locacaoEditando.valorDiaria;
  } else {
    const v = veiculosDisponiveis.find((x) => String(x.id) === f.veiculoId.value);
    diaria = v ? v.valorDiaria : null;
  }
  const dias = diffDias(f.dataRetirada.value, f.dataPrevistaDevolucao.value);
  if (diaria == null || dias == null || dias < 1) { box.textContent = ''; return; }
  const pct = dias >= 15 ? 0.15 : dias >= 7 ? 0.10 : 0;
  const sub = Math.round(diaria * dias * 100) / 100;
  const desc = Math.round(sub * pct * 100) / 100;
  box.textContent = `${dias} diária(s) × ${money(diaria)} = ${money(sub)}` +
    (pct ? ` − desconto de ${pct * 100}% (${money(desc)})` : '') +
    ` → total previsto ${money(sub - desc)}. (Estimativa: a API valida as regras e calcula o valor oficial.)`;
}

['veiculoId', 'dataRetirada', 'dataPrevistaDevolucao'].forEach((n) =>
  formLocacao.elements[n].addEventListener('input', atualizarPreview));

$('#nova-locacao').addEventListener('click', () => abrirLocacao(null));

formLocacao.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErro(formLocacao);
  const f = formLocacao.elements;
  try {
    if (locacaoEditando) {
      await api(`/locacoes/${locacaoEditando.id}`, {
        method: 'PUT',
        body: {
          dataPrevistaDevolucao: f.dataPrevistaDevolucao.value || null,
          observacoes: textoOuNull(f.observacoes.value),
        },
      });
      toast('Locação atualizada');
    } else {
      await api('/locacoes', {
        method: 'POST',
        body: {
          clienteId: numOuNull(f.clienteId.value),
          veiculoId: numOuNull(f.veiculoId.value),
          dataRetirada: f.dataRetirada.value || null,
          dataPrevistaDevolucao: f.dataPrevistaDevolucao.value || null,
          observacoes: textoOuNull(f.observacoes.value),
        },
      });
      toast('Locação registrada');
    }
    dlgLocacao.close();
    carregarLocacoes();
  } catch (e) { mostrarErro(formLocacao, e); }
});

// ---- diálogo de devolução

const dlgDevolucao = $('#dlg-devolucao');
const formDevolucao = $('#form-devolucao');
ligarDialogo(dlgDevolucao);
let devolvendo = null;

async function abrirDevolucao(l) {
  devolvendo = l;
  formDevolucao.reset();
  limparErro(formDevolucao);
  const f = formDevolucao.elements;
  try {
    const v = await api(`/veiculos/${l.veiculoId}`);
    f.quilometragemFinal.value = v.quilometragem;
    f.dataDevolucao.value = hojeISO();
    $('#devolucao-info').textContent =
      `${l.veiculoDescricao} — cliente ${l.clienteNome}. Devolução prevista: ${dataBR(l.dataPrevistaDevolucao)}` +
      `${l.atrasada ? ' (ATRASADA)' : ''}. Km atual: ${v.quilometragem.toLocaleString('pt-BR')}.`;
    dlgDevolucao.showModal();
  } catch (e) { toast(erroTexto(e), 'error'); }
}

formDevolucao.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErro(formDevolucao);
  const f = formDevolucao.elements;
  try {
    const r = await api(`/locacoes/${devolvendo.id}/devolucao`, {
      method: 'POST',
      body: {
        dataDevolucao: f.dataDevolucao.value || null,
        quilometragemFinal: numOuNull(f.quilometragemFinal.value),
      },
    });
    dlgDevolucao.close();
    toast(`Devolução registrada. Valor final: ${money(r.valorFinal)}` +
      (r.multa > 0 ? ` (inclui multa/taxa de ${money(r.multa)})` : ''));
    carregarLocacoes();
  } catch (e) { mostrarErro(formDevolucao, e); }
});

// ------------------------------------------------------------ início

mostrarAba('dashboard');
