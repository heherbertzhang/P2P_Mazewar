import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by herbert on 2016-02-27.
 */
public class AvoidRepeatence {
    Map<String, NumberBlocks> processesSequencesMap;

    public AvoidRepeatence() {
        processesSequencesMap = new Hashtable<>(15);
    }
    public void addProccess(String name){
        processesSequencesMap.put(name, new NumberBlocks());
    }

    public boolean checkRepeatenceForProcess(String name, int sequenceNumber){
        return processesSequencesMap.get(name).checkRepeatence(sequenceNumber);
    }
/*
    public static void main(String args[]){
        NumberBlocks blocks = new NumberBlocks();
        blocks.insertNumber(5);
        System.out.println(blocks.toString());
        blocks.insertNumber(6);
        System.out.println(blocks.toString());
        blocks.insertNumber(4);
        System.out.println(blocks.toString());
        blocks.insertNumber(1);
        System.out.println(blocks.toString());
        blocks.insertNumber(2);
        System.out.println(blocks.toString());
        blocks.insertNumber(3);
        System.out.println(blocks.toString());
        System.out.println(blocks.checkRepeatence(5));
        System.out.println(blocks.toString());
        System.out.println(blocks.checkRepeatence(9));
        System.out.println(blocks.toString());

    }
*/
}
class NumberBlocks {
    List<ContinuousBlock> blocks;

    @Override
    public String toString() {
        String s = "| ";
        for( ContinuousBlock b : blocks){
            s += b.toString() + " ";
        }
        s+="|";
        return s;
    }

    public NumberBlocks() {
        blocks = new LinkedList<>();
        blocks.add(new ContinuousBlock(0,0));
    }

    public boolean checkRepeatence(int number){
        for(ContinuousBlock block : blocks){
            if(block.checkIfInRange(number)){
                return true;
            }
        }
        //not in any block, merge in to other block
        this.insertNumber(number);
        return false;
    }

    public void insertNumber(int number){
        int size = blocks.size();
        for(int i = 0; i < size; i++){
            ContinuousBlock b = blocks.get(i);
            if(number > b.max){
                if(i == size-1){
                    //try to merge
                    ContinuousBlock newBlock = new ContinuousBlock(number, number);
                    ContinuousBlock previousBlock = blocks.get(i);
                    if(previousBlock.mergeOtherLargerBlock(newBlock)){

                    }
                    else {
                        //append at the end
                        blocks.add(new ContinuousBlock(number, number));
                    }
                    break;
                }
                continue;
            }
            else if(number < b.min){
                //found place to insert block
                //check previous and next blocks
                ContinuousBlock newBlock = new ContinuousBlock(number, number);
                ContinuousBlock previousBlock = blocks.get(i-1);
                ContinuousBlock nextBlock = blocks.get(i);
                if(!(previousBlock.mergeOtherLargerBlock(newBlock))){
                    //not success
                    //merge next block
                    if(nextBlock != null) {
                        if (newBlock.mergeOtherLargerBlock(nextBlock)) {
                            //success next block
                            blocks.remove(i);
                            blocks.add(i, newBlock);
                        } else {
                            //cannot merge with both blocks, directly insert into the block
                            blocks.add(i, newBlock);
                        }
                    }
                    else {//at the end
                        blocks.add(newBlock); //append at the end
                    }
                }
                else {
                    //success
                    //merge next block
                    if(nextBlock != null) {
                        if (previousBlock.mergeOtherLargerBlock(nextBlock)) {
                            //success mext block
                            blocks.remove(i);
                        }
                    }
                }
                break;
            }
        }
    }
}
class ContinuousBlock{
    public int min;
    public int max;

    @Override
    public String toString() {
        return "("+min+", "+max+")";
    }

    public ContinuousBlock(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean mergeOtherLargerBlock(ContinuousBlock other){

        if(this.max + 1 == other.min){
            this.max = other.max;
            return true;
        }
        else{
            return false;
        }
    }
    public boolean checkIfInRange(int number){
        return (number >= min && number <= max);
    }
}