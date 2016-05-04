//package agentes;

import jade.core.AID;

import java.util.LinkedList;

/**
 *
 * @author icarli
 */
public class Subasta {
    private String titulo;
    private AID GanadorAnterior;
    private AID GanadorActual;
    private int precio;
    private int minimo;
    private int incremento;
    private int triunfo;
    private LinkedList<AID> pujadores;
    
    public Subasta(String titulo, int precio, int incremento, int minimo) {
        this.titulo = titulo;
        this.GanadorActual = null;
        this.GanadorAnterior= null;
        this.precio = precio;
        this.incremento = incremento;
        this.pujadores=new LinkedList();
        this.triunfo=0;
        this.minimo=minimo;
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

    public AID getGanadorAnterior() {
        return GanadorAnterior;
    }

    public void setGanadorAnterior(AID GanadorAnterior) {
        this.GanadorAnterior = GanadorAnterior;
    }

    public AID getGanadorActual() {
        return GanadorActual;
    }

    public void setGanadorActual(AID GanadorActual) {
        this.GanadorActual = GanadorActual;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public int getIncremento() {
        return incremento;
    }

    public int getMinimo() {
        return minimo;
    }

    public void setMinimo(int minimo) {
        this.minimo = minimo;
    }

    public void setIncremento(int incremento) {
        this.incremento = incremento;
    }

    public int getTriunfo() {
        return triunfo;
    }

    public void setTriunfo(int triunfo) {
        this.triunfo = triunfo;
    }
    
}
