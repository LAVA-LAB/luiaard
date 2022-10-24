CONF="-ci None --output_type pdf --verbose --save"
python plot_strength.py ../results/grid-strength-eval/GRID/ $CONF -o plots/strength_GRID
echo "finished"
