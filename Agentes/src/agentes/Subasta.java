//package agentes;

import jade.core.AID;
import java.util.LinkedList;

/**
 *
 * @author icarli
 */
public class Subasta {
    private String titulo;
    private AID Ganador;
    private int precioInicial;
    private int precioActual;
    private int incremento;
    private LinkedList<AID> pujadores;
    
    public Subasta(String titulo, AID Ganador, int precioInicial, int precioActual, int incremento) {
        this.titulo = titulo;
        this.Ganador = Ganador;
        this.precioInicial = precioInicial;
        this.precioActual = precioActual;
        this.incremento = incremento;
        this.pujadores=new LinkedList();
    }

    public LinkedList<AID> getPujadores() {
        return pujadores;
    }

    public void setPujadores(LinkedList<AID> pujadores) {
        this.pujadores = pujadores;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public AID getGanador() {
        return Ganador;
    }

    public void setGanador(AID Ganador) {
        this.Ganador = Ganador;
    }

    public int getPrecioInicial() {
        return precioInicial;
    }

    public void setPrecioInicial(int precioInicial) {
        this.precioInicial = precioInicial;
    }

    public int getPrecioActual() {
        return precioActual;
    }

    public void setPrecioActual(int precioActual) {
        this.precioActual = precioActual;
    }

    public int getIncremento() {
        return incremento;
    }

    public void setIncremento(int incremento) {
        this.incremento = incremento;
    }
}
