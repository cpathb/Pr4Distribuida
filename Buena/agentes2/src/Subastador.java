//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.LinkedList;

public class Subastador extends Agent{
    // La lista de objetos a subastar
    private HashMap<String, Subasta> subastas;
    // Interfaz de usuario para el agente
    //private interfazSubastador interfaz;

    // Lista de los Subastadores conocidos
    private AID[] pujadores;


    // Método para la inicialización del agente
    protected void setup() {
        // Creamos la lista donde estarán los objetos a subastar
        subastas = new HashMap();
        subastas.put("A", new Subasta("A",10,10,40));
        subastas.put("B", new Subasta("B",100,15,520));
        // Creamos y mostramos la interfaz de usuario
        //interfaz = new interazSubastador(this);
        //interfaz.showGui(); o interfaz.mostrar(); // Comprobar si el showGUI existe

        // Añadimos un comportamiento TickerBehaviour que planifique los envios de peticiones de participacion a los pujadores cada cierto tiempo
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick(){
                int i=0;
                // Consultamos con el servicio de páginas amarillas para conocer los subastadores
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Subastador");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println("Se encontraron los siguientes pujadores:");
                    pujadores = new AID[result.length];
                    while(i < result.length){
                        pujadores[i] = result[i].getName();
                        System.out.println(pujadores[i].getName());
                        i++;
                    }
                    if(!subastas.isEmpty()){
                        myAgent.addBehaviour(new manejadorSubastas());
                    }
                }
                catch (FIPAException fe){
                    fe.printStackTrace();
                }
            }
        });
    }

    // Acciones a ejecutar al invocar al doDelete()
    protected void takeDown() {
        // Cerramos la interfaz
        //interfaz.dispose();

        // Mensaje de sálida
        System.out.println("Subastador " + getAID().getName() + " finalizando");
    }

    public void crearSubastas(final String titulo, final int precio, int incremento){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(!subastas.containsKey(titulo)){
                    subastas.put(titulo, new Subasta(titulo,precio,precio,incremento));
                    System.out.println(titulo + " insertado en las subastas. Precio de sálida = " + precio);
                }
                else{
                    System.out.println("El libro '"+titulo + "' ya está a subasta");
                }
            }
        });
    }
}