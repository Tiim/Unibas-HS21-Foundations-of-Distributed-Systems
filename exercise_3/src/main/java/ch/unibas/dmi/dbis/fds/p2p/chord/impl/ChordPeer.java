package ch.unibas.dmi.dbis.fds.p2p.chord.impl;

import ch.unibas.dmi.dbis.fds.p2p.chord.api.*;
import ch.unibas.dmi.dbis.fds.p2p.chord.api.ChordNetwork;
import ch.unibas.dmi.dbis.fds.p2p.chord.api.data.Identifier;
import ch.unibas.dmi.dbis.fds.p2p.chord.api.data.IdentifierCircle;
import ch.unibas.dmi.dbis.fds.p2p.chord.api.data.IdentifierCircularInterval;
import ch.unibas.dmi.dbis.fds.p2p.chord.api.math.CircularInterval;

import java.util.Random;

import static ch.unibas.dmi.dbis.fds.p2p.chord.api.data.IdentifierCircularInterval.createOpen;

/**
 * TODO: write JavaDoc
 *
 * @author loris.sauter
 */
public class ChordPeer extends AbstractChordPeer {
  /**
   *
   * @param identifier
   * @param network
   */
  protected ChordPeer(Identifier identifier, ChordNetwork network) {
    super(identifier, network);
  }

  /**
   * Asks this {@link ChordNode} to find {@code id}'s successor {@link ChordNode}.
   *
   * Defined in [1], Figure 4
   *
   * @param caller The calling {@link ChordNode}. Used for simulation - not part of the actual chord definition.
   * @param id The {@link Identifier} for which to lookup the successor. Does not need to be the ID of an actual {@link ChordNode}!
   * @return The successor of the node {@code id} from this {@link ChordNode}'s point of view
   */
  @Override
  public ChordNode findSuccessor(ChordNode caller, Identifier id) {
    var pred = findPredecessor(caller, id);
    return pred.successor();
  }

  /**
   * Asks this {@link ChordNode} to find {@code id}'s predecessor {@link ChordNode}
   *
   * Defined in [1], Figure 4
   *
   * @param caller The calling {@link ChordNode}. Used for simulation - not part of the actual chord definition.
   * @param id The {@link Identifier} for which to lookup the predecessor. Does not need to be the ID of an actual {@link ChordNode}!
   * @return The predecessor of or the node {@code of} from this {@link ChordNode}'s point of view
   */
  @Override
  public ChordNode findPredecessor(ChordNode caller, Identifier id) {
    ChordNode n = this;

    var interval = IdentifierCircularInterval.createLeftOpen(n.id(), n.successor().id());
    while (!interval.contains(id)) {
      System.out.println("** " + n + " not in interval " + interval);
      n = n.closestPrecedingFinger(caller, id);
      interval = IdentifierCircularInterval.createLeftOpen(n.id(), n.successor().id());
    }

    System.out.println("* Find predecessor of index "+ id.getIndex() + " -> " + n + "   interval " + interval);
    return n;
  }

  /**
   * Return the closest finger preceding the  {@code id}
   *
   * Defined in [1], Figure 4
   *
   * @param caller The calling {@link ChordNode}. Used for simulation - not part of the actual chord definition.
   * @param id The {@link Identifier} for which the closest preceding finger is looked up.
   * @return The closest preceding finger of the node {@code of} from this node's point of view
   */
  @Override
  public ChordNode closestPrecedingFinger(ChordNode caller, Identifier id) {
    var m = this.getNetwork().getNbits();
    for(int i = m; i >= 1; i--) {
      var ft = this.getFingerTable().node(i);
      if (ft.isEmpty()) {
        continue;
      }
      var node = ft.get();
      var interval = IdentifierCircularInterval.createOpen(this.id(), id);
      if ( interval.contains(node.id()) ) {
        return node;
      }
    }
    return this;
  }

  /**
   * Called on this {@link ChordNode} if it wishes to join the {@link ChordNetwork}. {@code nprime} references another {@link ChordNode}
   * that is already member of the {@link ChordNetwork}.
   *
   * Required for static {@link ChordNetwork} mode. Since no stabilization takes place in this mode, the joining node must make all
   * the necessary setup.
   *
   * Defined in [1], Figure 6
   *
   * @param nprime Arbitrary {@link ChordNode} that is part of the {@link ChordNetwork} this {@link ChordNode} wishes to join.
   */
  @Override
  public void joinAndUpdate(ChordNode nprime) {
    var m = getNetwork().getNbits();
    System.out.println(this + " joins the network using " + nprime);
    if (nprime != null) {

      initFingerTable(nprime);
      updateOthers();
      var succ = successor();
      var keys = succ.keys();
      for (var key : keys) {
        if (key.hashCode() % (2^m) <= id().getIndex()) {
          var value = succ.lookup(this, key);
          store(this, key, value.get());
          succ.delete(this, key);
        }
      }


    } else {
      for (int i = 1; i <= getNetwork().getNbits(); i++) {
        System.out.printf("Finger Table %s[%d] = %s%n", this, i, this);
        this.fingerTable.setNode(i, this);
      }
      System.out.println("Predecessor of " + this + ": " + this);
      this.setPredecessor(this);
    }
    System.out.println();
  }

  /**
   * Called on this {@link ChordNode} if it wishes to join the {@link ChordNetwork}. {@code nprime} references
   * another {@link ChordNode} that is already member of the {@link ChordNetwork}.
   *
   * Required for dynamic {@link ChordNetwork} mode. Since in that mode {@link ChordNode}s stabilize the network
   * periodically, this method simply sets its successor and waits for stabilization to do the rest.
   *
   * Defined in [1], Figure 7
   *
   * @param nprime Arbitrary {@link ChordNode} that is part of the {@link ChordNetwork} this {@link ChordNode} wishes to join.
   */
  @Override
  public void joinOnly(ChordNode nprime) {
    setPredecessor(null);
    if (nprime == null) {
      this.fingerTable.setNode(1, this);
    } else {
      this.fingerTable.setNode(1, nprime.findSuccessor(this,this));
    }
  }

  /**
   * Initializes this {@link ChordNode}'s {@link FingerTable} based on information derived from {@code nprime}.
   *
   * Defined in [1], Figure 6
   *
   * @param nprime Arbitrary {@link ChordNode} that is part of the network.
   */
  private void initFingerTable(ChordNode nprime) {

    System.out.println("Initialize finger table of " + this);

    var circle = getNetwork().getIdentifierCircle();
    fingerTable.setNode(1, nprime.findSuccessor(this, circle.getIdentifierAt(fingerTable.start(1))));

    System.out.println("Predecessor of " + this + ": " + successor().predecessor());
    setPredecessor(successor().predecessor());
    System.out.println("Predecessor of " + successor() + ": " + this);
    successor().setPredecessor(this);

    var m = getNetwork().getNbits();
    for (int i = 1; i < m; i++) {
      var fstart = fingerTable.start(i + 1);
      var interval = IdentifierCircularInterval.createRightOpen(this.id(), fingerTable.node(i).get().id());
      if (interval.contains(circle.getIdentifierAt(fstart))) {
        System.out.printf("Finger Table %s[%d] %s = %s    (initFingerTable)%n", this, i, getFingerTableRange(i) , fingerTable.node(i).get());
        fingerTable.setNode(i + 1, fingerTable.node(i).get());
      } else {
        System.out.printf("Finger Table %s[%d] %s = %s    (initFingerTable)%n", this, i+1, getFingerTableRange(i+1) , nprime.findSuccessor(this, circle.getIdentifierAt(fingerTable.start(i + 1))));
        fingerTable.setNode(i + 1, nprime.findSuccessor(this, circle.getIdentifierAt(fingerTable.start(i + 1))));
      }
    }
  }

  /**
   * Updates all {@link ChordNode} whose {@link FingerTable} should refer to this {@link ChordNode}.
   *
   * Defined in [1], Figure 6
   */
  private void updateOthers() {

    System.out.println("Update other nodes than " + this);
    var circle = getNetwork().getIdentifierCircle();

    int m = getNetwork().getNbits();
    int n = this.id().getIndex();
    for (int i = 1; i <= m; i++) {
      var p = findPredecessor(this,
              circle.getIdentifierAt(n - (int) Math.pow(2, (i-1))));
      p.updateFingerTable(this, i);
    }
  }

  /**
   * If node {@code s} is the i-th finger of this node, update this node's finger table with {@code s}
   *
   * Defined in [1], Figure 6
   *
   * @param s The should-be i-th finger of this node
   * @param i The index of {@code s} in this node's finger table
   */
  @Override
  public void updateFingerTable(ChordNode s, int i) {
    finger().node(i).ifPresent(node -> {
      var interval = IdentifierCircularInterval.createRightOpen(node.id(), this.id());
      if (interval.contains(s.id())) {
        System.out.printf("Finger Table %s[%d] %s = %s%n",this,i, getFingerTableRange(i),s );
        fingerTable.setNode(i, s);
        var p = predecessor();
        p.updateFingerTable(s,i);
      }
    });
  }

  /**
   * Called by {@code nprime} if it thinks it might be this {@link ChordNode}'s predecessor. Updates predecessor
   * pointers accordingly, if required.
   *
   * Defined in [1], Figure 7
   *
   * @param nprime The alleged predecessor of this {@link ChordNode}
   */
  @Override
  public void notify(ChordNode nprime) {
    System.out.println(this + " notified by " + nprime);
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    if (this.predecessor() == null) {
      this.setPredecessor(nprime);
    }else {
      var interval = IdentifierCircularInterval.createOpen(this.predecessor().id(), this.id());
      /* TODO: Implementation required. Hint: Null check on predecessor! */
      if (interval.contains(nprime.id())) {
        this.setPredecessor(nprime);
      }
    }
  }

  /**
   * Called periodically in order to refresh entries in this {@link ChordNode}'s {@link FingerTable}.
   *
   * Defined in [1], Figure 7
   */
  @Override
  public void fixFingers() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING){

      int m = this.getNetwork().getNbits();
      int rand_i = (new Random().nextInt() % (m-1)) +1;

      var circle = getNetwork().getIdentifierCircle();
      fingerTable.setNode(rand_i, findSuccessor(this, circle.getIdentifierAt(fingerTable.start(1))));
    }
  }

  /**
   * Called periodically in order to verify this node's immediate successor and inform it about this
   * {@link ChordNode}'s presence,
   *
   * Defined in [1], Figure 7
   */
  @Override
  public void stabilize() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    ChordNode x = this.successor().predecessor();

    if (x != null) {
      var interval = IdentifierCircularInterval.createOpen(id(), successor().id());
      if (interval.contains(x.id())) {
        fingerTable.setNode(1, x);
      }
    }
    this.successor().notify(this);

  }

  /**
   * Called periodically in order to check activity of this {@link ChordNode}'s predecessor.
   *
   * Not part of [1]. Required for dynamic network to handle node failure.
   */
  @Override
  public void checkPredecessor() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    System.out.println("check predecessor " + this.predecessor());

    if (this.predecessor() != null && this.predecessor().status() == NodeStatus.OFFLINE) {
      this.setPredecessor(null);
    }
  }

  /**
   * Called periodically in order to check activity of this {@link ChordNode}'s successor.
   *
   * Not part of [1]. Required for dynamic network to handle node failure.
   */
  @Override
  public void checkSuccessor() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    if (this.successor() != null && this.successor().status() == NodeStatus.OFFLINE) {
      fingerTable.setNode(1, fingerTable.node(2).get());
    }
  }

  /**
   * Performs a lookup for where the data with the provided key should be stored.
   *
   * @return Node in which to store the data with the provided key.
   */
  @Override
  protected ChordNode lookupNodeForItem(String key) {
    int m = getNetwork().getNbits();
    int hash = key.hashCode() %  (int) Math.pow(2, m);
    var id = getNetwork().getIdentifierCircle().getIdentifierAt(hash);

    for (int i = 1; i < m; i++) {
      if (fingerTable.interval(i).contains(id) ) {
        return fingerTable.node(i).get();
      }
    }
    throw new RuntimeException("The finger table is not properly populated. HELPP!");
  }

  @Override
  public String toString() {
    return String.format("ChordPeer{id=%d}", this.id().getIndex());
  }

  public String getFingerTableRange(int i) {
    var pow = (int) Math.pow(2, getNetwork().getNbits());
    var start = (this.id().getIndex() + (int)Math.pow(2, i - 1)) % pow;
    var end = (this.id().getIndex() +  (int)Math.pow(2, i)) % pow;
    return String.format("[%s, %s)", start, end);
  }
}
