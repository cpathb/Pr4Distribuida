//package agentes;

import jade.core.AID;

import java.util.LinkedList;

/**
 *
 * @author icarli
 */
public class Subasta {
    private String titulo;
    private AID GanadorActual;
    private int precio;
    private int minimo;
    private int incremento;
    private LinkedList<AID> pujadores;

    
    public Subasta(String titulo, int precio, int incremento, int minimo) {
        this.titulo = titulo;
        this.GanadorActual = null;
        if(precio<1){ // Comprobamos que el precio no sea menor de 1€
            this.precio = 1;
        }
        else{
            this.precio = precio;
        }
        if(incremento<1){ // Comprobamos que el incremento no sea menor de 1€
            this.incremento = 1;
        }
        else{
            this.incremento = incremento;
        }
        this.minimo=minimo;
        this.pujadores=new LinkedList<AID>();


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
}
