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
    System.out.println("while");
    while (id.getIndex() > n.id().getIndex() && id.getIndex() < n.successor().id().getIndex()) {
      n = n.closestPrecedingFinger(caller, id);
    }

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
      var fingerTableEntry = ft.get();
      if ( fingerTableEntry.id().getIndex() < this.id().getIndex() && fingerTableEntry.id().getIndex() > id.getIndex() ) {
        return fingerTableEntry;
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
    if (nprime != null) {
      initFingerTable(nprime);
      updateOthers();
      var succ = successor();
      var keys = succ.keys();
      for (var key : keys) {
        if (key.hashCode()<= id().getIndex()) {
          var value = succ.lookup(this, key);
          store(this, key, value.get());
          succ.delete(this, key);
        }
      }


    } else {
      for (int i = 1; i <= getNetwork().getNbits(); i++) {
        this.fingerTable.setNode(i, this);
      }
      this.setPredecessor(this);
    }
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
    var circle = getNetwork().getIdentifierCircle();
    var id = circle.getIdentifierAt(fingerTable.start(1));
    fingerTable.setNode(1, nprime.findSuccessor(this,id));

    setPredecessor(fingerTable.successor().predecessor());
    fingerTable.successor().setPredecessor(this);

    var m = getNetwork().getNbits();
    for (int i = 1; i < m; i++) {
      var fstart = fingerTable.start(i+1);
      if (circle.getIdentifierAt(fstart).getIndex() >= this.id().getIndex() &&
              circle.getIdentifierAt(fstart).getIndex() < fingerTable.node(i).get().id().getIndex()
      ) {
        fingerTable.setNode(i+1, fingerTable.node(i).get());
      } else {
        fingerTable.setNode(i+1, nprime.findSuccessor(this, circle.getIdentifierAt(fingerTable.start(i+1))));
      }
    }
  }

  /**
   * Updates all {@link ChordNode} whose {@link FingerTable} should refer to this {@link ChordNode}.
   *
   * Defined in [1], Figure 6
   */
  private void updateOthers() {
    var circle = getNetwork().getIdentifierCircle();
    // Do not use find predecessor, use find_successor instead
    // See slides Self-Organisation p55

    int m = getNetwork().getNbits();
    for (int i = 1; i <= m; i++) {
      var p = findPredecessor(this,
              circle.getIdentifierAt(this.id().getIndex() - (int) Math.pow(2, (i-1))));
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

      if (s.id().getIndex() >= this.id().getIndex() && s.id().getIndex() < fingerTable.node(i).get().id().getIndex() ) {
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
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    /* TODO: Implementation required. Hint: Null check on predecessor! */
    if(this.predecessor()==null || CircularInterval.createOpen(this.predecessor().getIdentifier().getIndex(), this.getIdentifier().getIndex()).contains(nprime.getIdentifier().getIndex())){
      this.setPredecessor(nprime);
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
      int rand_i = new java.util.Random().nextInt();


      return;
    }

    /* TODO: Implementation required */
    throw new RuntimeException("This method has not been implemented!");
  }

  /**
   * Called periodically in order to verify this node's immediate successor and inform it about this
   * {@link ChordNode}'s presence,
   *
   * Defined in [1], Figure 7
   */
  @Override
  public void stabilize() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING){
      ChordNode x = this.successor().predecessor();
      if(CircularInterval.createOpen(this.getIdentifier().getIndex(), this.successor().getIdentifier().getIndex()).contains(x.getIdentifier().getIndex())){
        fingerTable.setNode(1, x);

      }
      this.successor().notify(this);
    }

    /* TODO: Implementation required.*/
  //  throw new RuntimeException("This method has not been implemented!");
  }

  /**
   * Called periodically in order to check activity of this {@link ChordNode}'s predecessor.
   *
   * Not part of [1]. Required for dynamic network to handle node failure.
   */
  @Override
  public void checkPredecessor() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;

    /* TODO: Implementation required. Hint: Null check on predecessor! */
    throw new RuntimeException("This method has not been implemented!");
  }

  /**
   * Called periodically in order to check activity of this {@link ChordNode}'s successor.
   *
   * Not part of [1]. Required for dynamic network to handle node failure.
   */
  @Override
  public void checkSuccessor() {
    if (this.status() == NodeStatus.OFFLINE || this.status() == NodeStatus.JOINING) return;
    /* TODO: Implementation required. Hint: Null check on predecessor! */
    throw new RuntimeException("This method has not been implemented!");
  }

  /**
   * Performs a lookup for where the data with the provided key should be stored.
   *
   * @return Node in which to store the data with the provided key.
   */
  @Override
  protected ChordNode lookupNodeForItem(String key) {
    /* TODO: Implementation required. Hint: Null check on predecessor! */
    throw new RuntimeException("This method has not been implemented!");
  }

  @Override
  public String toString() {
    return String.format("ChordPeer{id=%d}", this.id().getIndex());
  }
}
