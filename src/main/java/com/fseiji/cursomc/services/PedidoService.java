package com.fseiji.cursomc.services;

import java.util.Date;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fseiji.cursomc.domain.ItemPedido;
import com.fseiji.cursomc.domain.PagamentoComBoleto;
import com.fseiji.cursomc.domain.Pedido;
import com.fseiji.cursomc.domain.enums.EstadoPagamento;
import com.fseiji.cursomc.repositories.ItemPedidoRepository;
import com.fseiji.cursomc.repositories.PagamentoRepository;
import com.fseiji.cursomc.repositories.PedidoRepository;
import com.fseiji.cursomc.services.exceptions.ObjectNotFoundException;

@Service
public class PedidoService {

	@Autowired
	private PedidoRepository repo;

	@Autowired
	private PagamentoRepository pagamentoRepository;

	@Autowired
	private ItemPedidoRepository itemPedidoRepository;
	
	@Autowired
	private ProdutoService produtoService;

	@Autowired
	private BoletoService boletoService;

	public Pedido find(Integer id) {
		Optional<Pedido> obj = this.repo.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Pedido.class.getName()));
	}

	@Transactional
	public Pedido insert(Pedido obj) {
		obj.setId(null);
		obj.setInstante(new Date());
		obj.getPagamento().setEstado(EstadoPagamento.PENDENTE);
		obj.getPagamento().setPedido(obj);
		if (obj.getPagamento() instanceof PagamentoComBoleto) {
			PagamentoComBoleto pgto = (PagamentoComBoleto) obj.getPagamento();
			this.boletoService.preencherPagamentoComBoleto(pgto, obj.getInstante());
		}
		obj = this.repo.save(obj);
		this.pagamentoRepository.save(obj.getPagamento());
		for (ItemPedido ip : obj.getItens()) {
			ip.setDesconto(0.0);
			ip.setPreco(this.produtoService.find(ip.getProduto().getId()).getPreco());
			ip.setPedido(obj);
		}
		this.itemPedidoRepository.saveAll(obj.getItens());
		return obj;
	}
}
