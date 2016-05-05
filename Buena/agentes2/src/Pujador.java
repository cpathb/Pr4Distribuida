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
        objetivos.put("A",40); // Añadimos un objeto para pujar
        objetivos.put("B",290);
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

        // Añadimos un comportamiento TickerBehaviour que planifique los envios de peticiones de participacion a los pujadores cada cierto tiempo
        addBehaviour(new consultas());

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
                if(msg.getPerformative()==ACLMessage.CFP){ // Propuesta para pujar en una subasta
                    String contenido = msg.getContent(); // Obtenemos el título del objeto del mensaje
                    String[] partes=contenido.split("\\$");
                    String titulo=partes[0];
                    int precio= Integer.parseInt(partes[1]);
                    ACLMessage respuesta = msg.createReply(); // Creamos la respuesta
                    // Obtenemos el precio del libro por el que se pregunta
                    if (objetivos.containsKey(titulo)) { // Si el objeto está entre nuestros objetivos
                        if(precio<=objetivos.get(titulo)){ // Podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.PROPOSE); // Añadimos el tipo del mensaje
                            respuesta.setContent(titulo+"$"+precio); // Añadimos la puja al contenido
                            myAgent.send(respuesta); // Enviamos la respuesta
                            //System.out.println(myAgent.getName()+" enviada oferta por "+titulo+" de "+precio);////////////////////////////////////////////////////////////
                        }
                        else{ // No podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.REFUSE);
                            respuesta.setConversationId("Baja");
                            respuesta.setContent(titulo+"$"+precio);
                            myAgent.send(respuesta); // Enviamos la respuesta
                            //System.out.println(myAgent.getName()+" rechazada oferta por "+titulo+" de "+precio);////////////////////////////////////////////////////////
                        }
                    }
                    else { // Si no nos interesa el objeto
                        respuesta.setPerformative(ACLMessage.REFUSE);
                        respuesta.setContent(titulo+"$"+precio);
                        myAgent.send(respuesta); // Enviamos la respuesta
                        //System.out.println(myAgent.getName()+" no me interesa "+titulo);//////////////////////////////////////////////////////////////////////////////////
                    }
                }
                if(msg.getPerformative()==ACLMessage.REQUEST){ // Ganamos una subasta
                    String contenido = msg.getContent(); // Obtenemos el título del objeto del mensaje
                    String[] partes=contenido.split("\\$");
                    String titulo=partes[0];
                    int precio= Integer.parseInt(partes[1]);
                    objetivos.remove(titulo);
                    System.out.println("Has ganado la puja para: "+titulo+" con un precio de "+precio);
                }
                if(msg.getPerformative()==ACLMessage.INFORM){ // Se acabo una ronda de pujas de una subasta o nos hemos retirado de una puja
                    if(msg.getConversationId().compareTo("Ronda")==0){ // Fin de ronda
                        String contenido = msg.getContent();
                        String[] partes=contenido.split("\\$");
                        String titulo=partes[0];
                        int precio= Integer.parseInt(partes[1]);
                        String ganador=partes[2];
                        if(ganador.compareTo(myAgent.getName())==0){ // Comprobamos si somos nosotros los ganadores
                            System.out.println("Se ha acabado una ronda de la puja del subastador "+msg.getSender().getName()+", se ha ganado la ronda para: "+titulo+" con una puja de "+precio);
                        }
                        else{
                            System.out.println("Se ha acabado una ronda de la puja del subastador "+msg.getSender().getName()+", "+ganador+" ha ganado la ronda para: "+titulo+" con una puja de "+precio);
                        }
                    }
                    if(msg.getConversationId().compareTo("Baja")==0) { // Nos hemos dado de baja
                        String contenido = msg.getContent();
                        String[] partes=contenido.split("\\$");
                        String titulo=partes[0];
                        int precio= Integer.parseInt(partes[1]);
                        System.out.println("Se ha dado de baja en la subasta "+msg.getSender().getName()+" para: "+titulo+" cuando se pujaba a "+precio);
                    }
                }
            }
            else { // Si no se recibieron solicitudes de información pasamos al siguiente comportamiento
                block();
            }
        }
    }

}
