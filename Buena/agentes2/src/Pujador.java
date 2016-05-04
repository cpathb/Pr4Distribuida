//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;

public class Pujador extends Agent{
    // Titulos de los objeto a comprar y su precio
    private HashMap<String,Integer> objetivos;

    // Interfaz de usuario para el agente pujador
    //private interfazPujador interfaz;

    // Lista de los Subastadores conocidos
    private AID[] subastadores;

    // Método para la inicialización del agente
    protected void setup(){
        // Creamos la lista donde estarán los objetos a subastar
        objetivos = new HashMap();
        objetivos.put("A",100); // Añadimos un objeto para pujar
        // Creamos y mostramos la interfaz de usuario
        //interfaz = new interazSPujador(this);
        //interfaz.showGui(); o interfaz.mostrar(); // Comprobar si el showGUI existe

        // Registramos al pujador en el servicio de paginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Pujador");
        sd.setName("Agente-Pujador-JADE");
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }
    }

    // Acciones a ejecutar al invocar el doDelete()
    protected void takeDown() {
        // Deregistramos el agente pujador de las páginas amarillas
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Cerramos la interfaz
        //interfaz.dispose();

        // Imprimimos un mensaje de despedida
        System.out.println("Pujador " + getAID().getName() + " finalizando");
    }

    private class consultas extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP),MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) { // Comprobamos si se ha recibido una consulta por un objeto de la subasta
                String contenido = msg.getContent(); // Obtenemos el título del objeto del mensaje
                String[] partes=contenido.split("\\$");
                String titulo=partes[0];
                int precio= Integer.parseInt(partes[1]);
                if(msg.getPerformative()==ACLMessage.CFP){ // Propuesta para pujar en una subasta
                    ACLMessage respuesta = msg.createReply(); // Creamos la respuesta
                    // Obtenemos el precio del libro por el que se pregunta
                    if (objetivos.containsKey(titulo)) { // Si el objeto está entre nuestros objetivos
                        if(precio<=objetivos.get(titulo)){ // Podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.PROPOSE); // Añadimos el tipo del mensaje
                            respuesta.setContent(titulo+"$"+precio); // Añadimos la puja al contenido
                            myAgent.send(respuesta); // Enviamos la respuesta
                        }
                        else{ // No podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.REFUSE);
                            respuesta.setContent("No me interesa la subasta de '"+titulo+"'");
                            myAgent.send(respuesta); // Enviamos la respuesta
                        }
                    }
                    else { // Si no nos interesa el objeto
                        respuesta.setPerformative(ACLMessage.REFUSE);
                        respuesta.setContent("No me interesa la subasta de '"+titulo+"'");
                        myAgent.send(respuesta); // Enviamos la respuesta
                    }
                }
                if(msg.getPerformative()==ACLMessage.REQUEST){ // Ganamos una subasta
                    objetivos.remove(titulo);
                    System.out.println("Has ganado la puja para: "+titulo);
                }
                if(msg.getPerformative()==ACLMessage.INFORM){ // Perdimos una subasta
                    System.out.println("Se ha acabado una puja del subastador"+msg.getSender().getName()+", se ha perdido la puja para: "+titulo);
                }
            }
            else { // Si no se recibieron solicitudes de información pasamos al siguiente comportamiento
                block();
            }
        }
    }

}
