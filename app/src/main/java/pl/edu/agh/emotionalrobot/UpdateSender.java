package pl.edu.agh.emotionalrobot;

class UpdateSender {
    private Options options;

    public UpdateSender(Options options){
        this.options = options;
    }
    public static class Options{
        int interval;
        public Options(int interval){
            this.interval = interval;
        }
    }
}
